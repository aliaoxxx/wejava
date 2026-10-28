package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.security.model.LoginSession;
import com.wejava.system.converter.SysMenuConverter;
import com.wejava.system.mapper.SysMenuMapper;
import com.wejava.system.mapper.SysRoleMapper;
import com.wejava.system.model.entity.SysMenu;
import com.wejava.system.model.form.SysMenuForm;
import com.wejava.system.model.query.SysMenuQuery;
import com.wejava.system.model.vo.MetaVO;
import com.wejava.system.model.vo.RoleMenuTreeVO;
import com.wejava.system.model.vo.RouterVO;
import com.wejava.system.model.vo.SysMenuVO;
import com.wejava.system.service.SysMenuService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private static final String TYPE_DIR = "M";
    private static final String TYPE_MENU = "C";
    private static final String TYPE_BUTTON = "F";

    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuConverter converter;

    // ---------------- 动态路由 ----------------

    @Override
    public List<RouterVO> routers(LoginSession session) {
        if (session == null) {
            return List.of();
        }
        List<SysMenu> menus = session.isSuperAdmin()
                ? menuMapper.selectAllRouters()
                : menuMapper.selectRoutersByUserId(session.getUserId());
        return buildRouters(menus, 0L, "", "");
    }

    private List<RouterVO> buildRouters(List<SysMenu> menus, Long parentId, String parentName, String parentPath) {
        List<RouterVO> result = new ArrayList<>();
        menus.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .sorted(Comparator.comparing(SysMenu::getOrderNum, Comparator.nullsLast(Integer::compareTo)))
                .forEach(menu -> {
                    RouterVO router = new RouterVO();
                    String name = parentName + pascal(menu.getPath());
                    router.setName(name);
                    router.setPath(menu.getPath());

                    MetaVO meta = new MetaVO();
                    meta.setTitle(menu.getMenuName());
                    meta.setIcon(menu.getIcon());
                    meta.setHidden(menu.getVisible() != null && menu.getVisible() == 1);
                    meta.setCache(menu.getIsCache() != null && menu.getIsCache() == 1);
                    router.setMeta(meta);

                    if (TYPE_DIR.equals(menu.getMenuType())) {
                        String fullPath = join(parentPath, menu.getPath());
                        List<RouterVO> children = buildRouters(menus, menu.getMenuId(), name, fullPath);
                        router.setComponent("Layout");
                        router.setChildren(children);
                        if (!children.isEmpty()) {
                            router.setRedirect(fullPath + "/" + children.get(0).getPath());
                        }
                    } else {
                        router.setComponent(menu.getComponent());
                    }
                    result.add(router);
                });
        return result;
    }

    // ---------------- 管理 ----------------

    @Override
    public List<SysMenuVO> tree(SysMenuQuery query) {
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .like(StringUtils.hasText(query.getMenuName()), SysMenu::getMenuName, query.getMenuName())
                .eq(query.getStatus() != null, SysMenu::getStatus, query.getStatus())
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum));
        return buildMenuTree(converter.toVOList(menus), 0L);
    }

    @Override
    public SysMenuVO getById(Long menuId) {
        return converter.toVO(requireMenu(menuId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysMenuForm form) {
        validate(form);
        SysMenu entity = converter.toEntity(form);
        entity.setMenuId(null);
        if (entity.getParentId() == null) {
            entity.setParentId(0L);
        }
        if (entity.getOrderNum() == null) {
            entity.setOrderNum(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getVisible() == null) {
            entity.setVisible(0);
        }
        menuMapper.insert(entity);
        return entity.getMenuId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysMenuForm form) {
        SysMenu entity = requireMenu(form.getMenuId());
        validate(form);
        if (form.getMenuId().equals(form.getParentId())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "上级菜单不能是自己");
        }
        converter.update(form, entity);
        menuMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long menuId) {
        requireMenu(menuId);
        long children = menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, menuId));
        if (children > 0) {
            throw new BizException(ErrorCode.HAS_CHILDREN);
        }
        if (menuMapper.countRoleRef(menuId) > 0) {
            throw new BizException(ErrorCode.HAS_REFERENCE);
        }
        menuMapper.deleteById(menuId);
    }

    @Override
    public List<SysMenuVO> treeSelect() {
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .in(SysMenu::getMenuType, TYPE_DIR, TYPE_MENU)
                .eq(SysMenu::getStatus, 0)
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum));
        return buildMenuTree(converter.toVOList(menus), 0L);
    }

    @Override
    public RoleMenuTreeVO roleMenuTree(Long roleId) {
        List<SysMenu> menus = menuMapper.selectList(
                Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getOrderNum));
        return new RoleMenuTreeVO(
                buildMenuTree(converter.toVOList(menus), 0L), roleMapper.selectMenuIdsByRoleId(roleId));
    }

    // ---------------- 内部 ----------------

    private List<SysMenuVO> buildMenuTree(List<SysMenuVO> all, Long parentId) {
        List<SysMenuVO> result = new ArrayList<>();
        for (SysMenuVO node : all) {
            if (!Objects.equals(node.getParentId(), parentId)) {
                continue;
            }
            node.setChildren(buildMenuTree(all, node.getMenuId()));
            result.add(node);
        }
        return result;
    }

    private void validate(SysMenuForm form) {
        if (TYPE_BUTTON.equals(form.getMenuType()) && !StringUtils.hasText(form.getPerms())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "按钮类型必须填写权限字符");
        }
        if (TYPE_MENU.equals(form.getMenuType()) && !StringUtils.hasText(form.getComponent())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "菜单类型必须填写组件路径");
        }
    }

    private SysMenu requireMenu(Long menuId) {
        SysMenu menu = menuMapper.selectById(menuId);
        if (menu == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return menu;
    }

    private String join(String parentPath, String path) {
        if (path == null) {
            return parentPath;
        }
        if (path.startsWith("/")) {
            return path;
        }
        return parentPath.isEmpty() ? "/" + path : parentPath + "/" + path;
    }

    private String pascal(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("[^A-Za-z0-9]+")) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
