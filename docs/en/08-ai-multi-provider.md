[中文](../08-AI多服务商设计.md) | English

# 08 · AI Multi-Provider Design

> Goal: turn "one DeepSeek endpoint" into "several domestic model endpoints, switchable in chat".
> Status: **implemented** (V15 migration + multi-endpoint management + chat switching).
> Deviations from the original draft are marked "implementation note".

---

## 1. Current state

| Item | Today | Problem |
|---|---|---|
| Storage | `sys_ai_config` already has provider / model / base_url / api_key / temperature / max_tokens | Only one row is used: `selectOne(... .last("LIMIT 1"))` |
| `provider` column | Present | Never used in logic — switching vendors still means editing base_url by hand |
| Client | `DeepSeekClient` posts to `baseUrl + "/chat/completions"` | **Already speaks the OpenAI-compatible protocol**; only the name says DeepSeek |
| base_url joining | Naive string concatenation | No trailing-slash handling — Zhipu's documented `.../v4/` becomes `//chat/completions` |
| Frontend | Single form | Cannot manage more than one endpoint |

**Key conclusion**: the protocol layer does not need a rewrite. Every domestic vendor we care about
(DeepSeek, Qwen, Zhipu, Kimi, Doubao, Qianfan, Hunyuan, Spark, SiliconFlow) exposes an
OpenAI-compatible API, so **one adapter covers them all**. The work is configuration model,
endpoints and UI.

## 2. Principles

1. **Adapt by protocol, not by vendor** — vendor differences live in configuration and presets, never in code branches.
2. **Never hardcode model names** — see the evidence below; the model comes from the user or from the vendor's model list.
3. **One row = one model endpoint** (provider + model + base_url + key + sampling params).
4. **Exactly one default** — chat uses the row with `is_default = 1` when no endpoint is specified.

> **Why "never hardcode model names"**
> While writing this design the vendor docs showed Zhipu at `glm-5.3` and Kimi at `K3.1 Pro`.
> Hardcoding `glm-4-plus` or `moonshot-v1-8k` would ship stale code on day one.

## 3. Data model (V15)

```sql
ALTER TABLE `sys_ai_config`
    ADD COLUMN `config_name` varchar(64)  NOT NULL DEFAULT '' COMMENT 'display name',
    ADD COLUMN `protocol`    varchar(32)  NOT NULL DEFAULT 'openai' COMMENT 'protocol: openai (only implementation today)',
    ADD COLUMN `is_default`  tinyint      NOT NULL DEFAULT 0 COMMENT 'endpoint used by chat by default',
    ADD COLUMN `sort`        int          NOT NULL DEFAULT 0 COMMENT 'list ordering',
    ADD COLUMN `remark`      varchar(200)          DEFAULT NULL COMMENT 'free-form note, e.g. vendor docs URL';

-- Give the existing row a name and make it the default so chat keeps working after the upgrade
UPDATE `sys_ai_config` SET `config_name` = CONCAT(`provider`, ' ', `model`)
WHERE `config_name` = '';
UPDATE `sys_ai_config` SET `is_default` = 1 ORDER BY `ai_config_id` LIMIT 1;
```

**Constraints**

- `is_default = 1` is unique; the service clears the flag on every row before setting a new one.
- The default row **cannot be deleted**, so chat never silently breaks.

## 4. API design

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/system/ai-config/list` | `system:ai:config` | List endpoints; keys returned masked only |
| GET | `/system/ai-config/{id}` | same | Single endpoint |
| POST | `/system/ai-config` | same | Create |
| PUT | `/system/ai-config/{id}` | same | Update (empty key keeps the existing one) |
| DELETE | `/system/ai-config/{id}` | same | Delete; rejected for the default row |
| PUT | `/system/ai-config/{id}/default` | same | Make default |
| GET | `/system/ai-config/providers` | same | Vendor presets (base_url + docs link) |
| GET | `/system/ai-config/{id}/models` | same | **Server-side proxy** for the vendor model list |
| POST | `/system/ai-config/{id}/test` | same | Connectivity test with latency and error reason |

**Two hard requirements**

1. **The model list must be proxied by the server** — the browser must never hold the API key and
   call the vendor directly, otherwise the key shows up in the network panel.
2. **Rate-limit the test and list endpoints** — reuse the existing `@RateLimit`; they look like a
   scanner to any security team.

Chat gains an optional `configId`:

```
POST /ai/chat/{sessionId}?configId=12      # explicit endpoint
POST /ai/chat/{sessionId}                  # default endpoint
```

`sys_ai_session` already has a `model` column; store `configId + model` **per message** so it is
possible to tell which endpoint produced which answer.

## 5. Backend layering

```
ai/
├── client/
│   ├── ChatClient.java              # interface: void stream(ChatRequest, Consumer<Chunk>)
│   ├── OpenAiCompatibleClient.java  # renamed from DeepSeekClient, logic unchanged
│   └── AiClientFactory.java         # picks an implementation by protocol
├── model/{entity,form,vo}/…         # add config_name / is_default / protocol / sort
├── service/AiConfigService.java     # add list / create / update / delete / setDefault / byId
└── controller/AiConfigController.java
```

**A bug to fix (already located)**

```java
// Today: naive concatenation; Zhipu's .../v4/ becomes .../v4//chat/completions
String url = baseUrl + "/chat/completions";

// Fix: normalise first
String url = stripTrailingSlash(baseUrl) + "/chat/completions";
```

**Two delta shapes the stream parser must accept**

```json
{"choices":[{"delta":{"content":"answer"}}]}           // regular
{"choices":[{"delta":{"reasoning_content":"thought"}}]} // DeepSeek / Zhipu reasoning models
```

Reasoning deltas are streamed to the UI but **not persisted** (they are large); only the final
answer is written to `sys_ai_message`.

## 6. Frontend

### 6.1 Config page (`views/ai/config.vue`)

Single form becomes **list + dialog**:

| Column | Content |
|---|---|
| Name | `config_name` |
| Vendor | provider (badge) |
| Model | model |
| Default | badge, clickable to switch |
| Status | enabled / disabled |
| Actions | edit / test / delete |

**Dialog fields**

| Field | Interaction |
|---|---|
| Vendor | dropdown including **Custom**; selecting one fills base_url and the docs link |
| Display name | text, defaults to the vendor name |
| Base URL | text, required when Custom |
| Model | input + **"Fetch models"** button (server proxy), or type it manually |
| API key | password field; empty on edit means "keep current", placeholder shows the mask |
| Temperature / max tokens | number inputs; temperature upper bound hinted per vendor (Zhipu 0–1, others 0–2) |
| Enabled / default | switches |

Two buttons at the bottom: **Test connection** (works without saving) and Save.

### 6.2 Chat page (`views/ai/index.vue`)

A model selector at the top:

- lists only `enabled = 1` endpoints
- defaults to `is_default`, remembers the user's last choice (localStorage)
- a change applies to the next message; history is unaffected

## 7. Compatibility checklist

| Issue | Handling |
|---|---|
| Trailing slash in base_url | Strip before appending `/chat/completions` |
| Model names drift per release | Never hardcode; offer "fetch models", fall back to manual input |
| Temperature ranges differ | Hint per vendor in the UI; backend only sanity-checks 0–2 |
| `reasoning_content` deltas | Accept in the stream parser, do not persist |
| Token usage | OpenAI needs `stream_options.include_usage`; DeepSeek/Zhipu return it by default. Parse defensively |
| 401 / 429 / insufficient balance | Map to clear Chinese messages; never surface raw upstream payloads |
| Timeout | The current 5 minutes is tight for long-context models; make it configurable (default 5 min, max 15 min) |

## 8. Vendor presets (base_url to be verified one by one)

| provider | Name | base_url | Status |
|---|---|---|---|
| `deepseek` | DeepSeek | `https://api.deepseek.com` | ✅ in production today |
| `zhipu` | Zhipu GLM | `https://open.bigmodel.cn/api/paas/v4` | ✅ confirmed in vendor docs |
| `qwen` | Qwen (Model Studio) | `https://dashscope.aliyuncs.com/compatible-mode/v1` | ⚠️ to verify |
| `kimi` | Kimi | `https://api.moonshot.cn/v1` | ⚠️ to verify (docs moved to platform.kimi.com) |
| `doubao` | Volcano Ark (Doubao) | `https://ark.cn-beijing.volces.com/api/v3` | ⚠️ to verify |
| `qianfan` | Qianfan (ERNIE) | `https://qianfan.baidubce.com/v2` | ⚠️ to verify |
| `hunyuan` | Hunyuan | `https://api.hunyuan.cloud.tencent.com/v1` | ⚠️ to verify |
| `spark` | iFlytek Spark | `https://spark-api-open.xf-yun.com/v1` | ⚠️ to verify |
| `siliconflow` | SiliconFlow | `https://api.siliconflow.cn/v1` | ⚠️ to verify |
| `custom` | Custom | user-supplied | — |

Presets are **form defaults only**; the stored row always holds the final values.

## 9. Security

- Keys keep using `AesUtils` (AES-GCM with `AI_CONFIG_SECRET`); list responses return masks only.
- Model listing and connectivity tests are issued **by the server**, so the key never reaches the browser.
- Both new endpoints carry `@RateLimit` so they cannot be used as a probing tool.
- Deleting the default endpoint, or disabling every endpoint, breaks chat — the service must block it
  with an explicit message.

## 10. Migration and rollback

| Scenario | Approach |
|---|---|
| Upgrade | Flyway V15 adds the columns; the existing row gets a name and becomes the default, chat is unaffected |
| Rollback | Old code ignores the new columns and keeps working; no SQL rollback needed |
| Data compatibility | All new columns have defaults, so `INSERT` statements that omit them still work |

## 11. Rollout order

| Phase | Work | Estimate |
|---|---|---|
| 1 | V15 migration + entity/VO/Form extension | 0.5 day |
| 2 | Multi-row config service + default handling + base_url normalisation | 1 day |
| 3 | Client rename + model-list / test endpoints | 1 day |
| 4 | Config page (list + dialog + test) | 1.5 days |
| 5 | Chat model selector + per-message configId | 0.5 day |
| 6 | Verify each vendor base_url, bilingual docs, regression | 1.5 days |

## 12. Acceptance criteria

- [ ] At least two vendor endpoints can be configured independently
- [ ] The chat page can switch models, effective from the next message
- [ ] "Fetch models" returns results on vendors that support it
- [ ] "Test connection" distinguishes 401 (bad key) / 404 (bad URL) / timeout
- [ ] Exactly one default endpoint, and it cannot be deleted
- [ ] After the upgrade the old config becomes the default and chat never breaks
- [ ] `docs/03`, `docs/06` and `README` updated in both languages
