package cn.crap.utils;

import java.util.List;

import cn.crap.dto.LoginInfoDto;
import cn.crap.dto.PickDto;
import cn.crap.enumeration.DataCeneterType;
import cn.crap.enumeration.DataType;
import cn.crap.enumeration.FontFamilyType;
import cn.crap.enumeration.InterfaceStatus;
import cn.crap.enumeration.MenuType;
import cn.crap.enumeration.ModuleStatus;
import cn.crap.enumeration.RequestMethod;
import cn.crap.enumeration.SettingType;
import cn.crap.enumeration.TrueOrFalse;
import cn.crap.enumeration.UserType;
import cn.crap.enumeration.WebPageType;
import cn.crap.framework.MyException;
import cn.crap.inter.service.IDataCenterService;
import cn.crap.inter.service.IErrorService;
import cn.crap.inter.service.IMenuService;
import cn.crap.inter.service.IRoleService;
import cn.crap.inter.service.IWebPageService;
import cn.crap.model.DataCenter;
import cn.crap.model.Error;
import cn.crap.model.Menu;
import cn.crap.model.Role;
import cn.crap.model.WebPage;

public class PickFactory {

	/**
	 * 
	 * @param picks 前端选项（非敏感信息）
	 * @param code 需要选着的pick代码
	 * @param key pick二级关键字（如类型、父节点等）
	 * @return
	 * @throws MyException 
	 */

	public static void getFrontPickList(List<PickDto> picks, String code, String key, 
			IMenuService menuService, IDataCenterService dataCenter, IErrorService errorService, IRoleService roleService, IWebPageService webPageService,IDataCenterService dataCenterService) throws MyException {
		
			PickDto pick = null;
			switch (code) {
				case "FRONTERRORMODULE": // 前端错误码模块列表
					for (DataCenter m : dataCenter.findByMap(Tools.getMap("parentId", "0", "type", "MODULE", "status", "1"), null, null)) {
						pick = new PickDto(m.getId(), m.getName());
						picks.add(pick);
					}
					return;
				case "REQUESTMETHOD": // 枚举 请求方式 post get
					for (RequestMethod status : RequestMethod.values()) {
						pick = new PickDto(status.name(), status.getName(), status.getName());
						picks.add(pick);
					}
					return;
					// 枚举 接口状态
				case "INTERFACESTATUS":
					for (InterfaceStatus status : InterfaceStatus.values()) {
						pick = new PickDto(status.getName(), status.name());
						picks.add(pick);
					}
					return;
				case "TRUEORFALSE":// 枚举true or false
					for (TrueOrFalse status : TrueOrFalse.values()) {
						pick = new PickDto(status.getName(), status.name());
						picks.add(pick);
					}
					return;
			}
			// 如果前端pick没有，则查询登陆用户的pick
			if(getUserPickList(picks, code, key, menuService, dataCenter, errorService, roleService, webPageService, dataCenterService)){
				return;
			}
			
			// 如果前端选项、登陆用户的pick没有，则查询后端选项
			getBackPickList(picks, code, key, menuService, dataCenter, errorService, roleService, webPageService);
	}
	
	/**
	 * 普通用户pick
	 * @param picks
	 * @param code
	 * @param key
	 * @param menuService
	 * @param dataCenter
	 * @param errorService
	 * @param roleService
	 * @param webPageService
	 * @throws MyException
	 */
	private static boolean getUserPickList(List<PickDto> picks, String code, String key, 
			IMenuService menuService, IDataCenterService dataCenter, IErrorService errorService, IRoleService roleService, IWebPageService webPageService,
			IDataCenterService dataCenterService) throws MyException {
		// 需要登陆才能
		LoginInfoDto user = Tools.getUser();
		if(user == null ){
			throw new MyException("000003");
		}
		// 管理员通过getBackPickList方法查询
		if(user.getType() != 1){
			return false;
		}
		PickDto pick = null;
		switch (code) {
			case "DATACENTER":// 所有数据
				// 如果用户为普通用户，则只能查看自己的模块
				List<String> moduleIds = dataCenterService.getList(  null, DataCeneterType.MODULE.name(), Tools.getUser().getId() );
				moduleIds.add("NULL");
				
				dataCenter.getDataCenterPick(picks, Tools.getMap("id|in", moduleIds) , "", Const.PRIVATE_MODULE , key,  Const.LEVEL_PRE , "", "");
				picks.add(0,new PickDto(Const.PRIVATE_MODULE, "根目录（用户）"));
				return true;
				// 枚举 模块类型（公开、私有）
			case "MODULESTATUS":
				for (ModuleStatus status : ModuleStatus.values()) {
					// 用户不能设置模块为推荐状态
					if(status.getName().equals("3"))
						continue;
					pick = new PickDto(status.getName(), status.name());
					picks.add(pick);
				}
				return true;
		}
		
		return false;
	}
	/**
	 * 
	 * @param picks 后台选项
	 * @param code 需要选着的pick代码
	 * @param key pick二级关键字（如类型、父节点等）
	 * @return
	 * @throws MyException 
	 */

	private static void getBackPickList(List<PickDto> picks, String code, String key, 
			IMenuService menuService, IDataCenterService dataCenter, IErrorService errorService, IRoleService roleService, 
			IWebPageService webPageService) throws MyException {
		PickDto pick = null;
		String preUrl = "";
		// 后端选项需要具有数据查看功能才能查看
		Tools.hasAuth(Const.AUTH_ADMIN,"");
		switch (code) {
		// 一级菜单
		case "MENU":
			for (Menu m : menuService.findByMap(Tools.getMap("parentId", "0"), null, null)) {
				pick = new PickDto(m.getId(), m.getMenuName());
				picks.add(pick);
			}
			return;
			
		// 权限
		case "AUTH":
			pick = new PickDto(DataType.VIEW.name() + "_0", DataType.VIEW.getName());
			picks.add(pick);
			pick = new PickDto(DataType.MODULE.name() + "_0", "项目管理");
			picks.add(pick);
			// 分割线
			pick = new PickDto(Const.SEPARATOR, "模块管理");
			picks.add(pick);
			dataCenter.getDataCenterPick(picks, null, "m_", "0", "MODULE", "", DataType.MODULE.name() + "_moduleId", "--【模块】");
			// 分割线
			pick = new PickDto(Const.SEPARATOR, "接口管理");
			picks.add(pick);
			dataCenter.getDataCenterPick(picks, null, "i_", "0", "MODULE", "", DataType.INTERFACE.name() + "_moduleId", "--【接口】");
			// 分割线
			pick = new PickDto(Const.SEPARATOR, "错误码管理");
			picks.add(pick);
			for (DataCenter m : dataCenter.findByMap(Tools.getMap("parentId", "0",  "type", "MODULE"), null, null)) {
				pick = new PickDto(m.getId(), DataType.ERROR.name() + "_" + m.getId(),
						m.getName() + "--【错误码】");
				picks.add(pick);
			}
			// 分割线
			pick = new PickDto(Const.SEPARATOR, "用户、菜单、角色、系统设置管理");
			picks.add(pick);
			pick = new PickDto(DataType.USER.name(), "用户管理");
			picks.add(pick);
			pick = new PickDto(DataType.ROLE.name(), "角色管理");
			picks.add(pick);
			pick = new PickDto(DataType.MENU.name(), "菜单管理");
			picks.add(pick);
			pick = new PickDto(DataType.SETTING.name(), "系统设置管理");
			picks.add(pick);
			pick = new PickDto(DataType.LOG);
			picks.add(pick);
			pick = new PickDto(DataType.SOURCE);
			picks.add(pick);
			// 分割线
			pick = new PickDto(Const.SEPARATOR, "数据字典");
			picks.add(pick);
			for (DataCenter m : dataCenter.findByMap(Tools.getMap("parentId", "0",  "type", "MODULE"), null, null)) {
				pick = new PickDto("w_d_" + m.getId(), DataType.DICTIONARY.name() + "_" + m.getId(),
						m.getName());
				picks.add(pick);
			}
			// 分割线
			pick = new PickDto(Const.SEPARATOR, "网站页面&文章管理");
			picks.add(pick);
			for (WebPageType w : WebPageType.values()) {
				if (w.equals(WebPageType.DICTIONARY))
					continue;
				pick = new PickDto("w_w_" + w.name(), w.name(), w.getName());
				picks.add(pick);
			}

			// 分割线
			pick = new PickDto(Const.SEPARATOR, "我的菜单");
			picks.add(pick);
			for (Menu m : menuService.findByMap(Tools.getMap("parentId", "0", "type", MenuType.BACK.name()), null,
					null)) {
				pick = new PickDto(m.getId(), m.getMenuName() + "--【菜单】");
				picks.add(pick);
			}
			return;
		
		// 角色
		case "ROLE":
			pick = new PickDto(Const.SUPER, "超级管理员");
			picks.add(pick);
			for (Role r : roleService.findByMap(null, null, null)) {
				pick = new PickDto(r.getId(), r.getRoleName());
				picks.add(pick);
			}
			return;
		
		// 顶级模块
		case "TOPMODULE":
			for (DataCenter m : dataCenter.findByMap(Tools.getMap("parentId", "0", "type", "MODULE"), null, null)) {
				pick = new PickDto(m.getId(), m.getName());
				picks.add(pick);
			}
			picks.add(0,new PickDto("0","顶级父类"));
			return;
		// 枚举 模块类型（公开、私有）
		case "MODULESTATUS":
			for (ModuleStatus status : ModuleStatus.values()) {
				pick = new PickDto(status.getName(), status.name());
				picks.add(pick);
			}
			return;
			
		// 枚举 webPage
		case "WEBPAGETYPE":
			for (WebPageType type : WebPageType.values()) {
				pick = new PickDto(type.name(), type.getName());
				picks.add(pick);
			}
			return;
		// 枚举 菜单类型
		case "MENUTYPE":
			for (MenuType type : MenuType.values()) {
				pick = new PickDto(type.name(), type.getName());
				picks.add(pick);
			}
			return;
		
		// 枚举 设置类型
		case "SETTINGTYPE":
			for (SettingType type : SettingType.values()) {
				pick = new PickDto(type.name(), type.getName());
				picks.add(pick);
			}
			return;
		case "DATATYPE":// 枚举 数据类型
			for (DataType status : DataType.values()) {
				pick = new PickDto(status.name(), status.getName());
				picks.add(pick);
			}
			return;
			
		case "ERRORCODE":// 错误码
			DataCenter module = dataCenter.get(key);
			while (module != null && !module.getParentId().equals("0")) {
				module = dataCenter.get(module.getParentId());
			}
			for (Error error : errorService.findByMap(
					Tools.getMap("moduleId", module == null ? "" : module.getId()), null, "errorCode asc")) {
				pick = new PickDto(error.getErrorCode(), error.getErrorCode() + "--" + error.getErrorMsg());
				picks.add(pick);
			}
			return;
		case "CATEGORY":
			int i = 0;
			@SuppressWarnings("unchecked")
			List<String> categorys = (List<String>) webPageService.queryByHql("select distinct category from WebPage", null);
			for (String w : categorys) {
				if (w == null)
					continue;
				i++;
				pick = new PickDto("cat_" + i, w, w);
				picks.add(pick);
			}
			return;
		case "MODELNAME":// 数据类型
			i = 0;
			@SuppressWarnings("unchecked")
			List<String> modelNames = (List<String>) webPageService.queryByHql("select distinct modelName from Log", null);
			for (String w : modelNames) {
				if (w == null)
					continue;
				i++;
				pick = new PickDto("modelName_" + i, w, w);
				picks.add(pick);
			}
			return;
		
		case "MENURUL":
			
			// 后台菜单url
			if(key.equals(MenuType.BACK.name())){
				pick = new PickDto(Const.SEPARATOR, "后台");
				picks.add(pick);
				// 后端错误码管理
				pick = new PickDto("h_e_0", "#/back/error/list", "错误码列表");
				picks.add(pick);
				// 后端用户管理
				pick = new PickDto("h_u_0", "#/back/user/list", "用户列表");
				picks.add(pick);
				// 后端角色管理
				pick = new PickDto("h_r_0", "#/back/role/list", "角色列表");
				picks.add(pick);
				// 后端PDF、DOC等文档管理
				pick = new PickDto("h_sorce_0", "#/back/source/list/0/根目录", "PDF、DOC等文档列表");
				picks.add(pick);
				// 后端系统设置
				pick = new PickDto("h_s_0", "#/back/setting/list/null", "系统设置列表");
				picks.add(pick);
				pick = new PickDto("h_l_0", "#/back/log/list", "日志列表");
				picks.add(pick);
				// 分割线
				pick = new PickDto(Const.SEPARATOR, "后台菜单列表");
				picks.add(pick);
				for (MenuType type : MenuType.values()) {
					pick = new PickDto("h_m_" + type.name(), "#/back/menu/list/0/" + type.name() + "/一级菜单",
							type.getName());
					picks.add(pick);
				}
				pick = new PickDto(Const.SEPARATOR, "后台数据字典&页面&文章管理");
				picks.add(pick);
				preUrl = "#/back/webPage/list/";
				
				for (WebPageType webPage : WebPageType.values()) {
					pick = new PickDto("h_" + webPage.name(), preUrl + webPage.name(), webPage.getName());
					picks.add(pick);
				}
				// 分割线
				pick = new PickDto(Const.SEPARATOR, "后台模块");
				picks.add(pick);
				// 后端接口&模块管理
				preUrl = "#/back/interface/list/";
				pick = new PickDto("h_0", preUrl + "0/无", "顶级模块");
				picks.add(pick);
				dataCenter.getDataCenterPick(picks, null, "h_", "0", "MODULE", "- - - ", preUrl + "moduleId/moduleName","");
				return;
			}
			
			// 前端菜单url
			else{
				pick = new PickDto(Const.SEPARATOR, "前端错误码");
				picks.add(pick);
				preUrl = "#/webError/list/";
				for (DataCenter m : dataCenter.findByMap(Tools.getMap("parentId", "0", "type", "MODULE"), null, null)) {
					pick = new PickDto("e_" + m.getId(), preUrl + m.getId(), m.getName());
					picks.add(pick);
				}
				// 分割线
				pick = new PickDto(Const.SEPARATOR, "前端模块");
				picks.add(pick);
				preUrl = "#/front/interface/list/";
				dataCenter.getDataCenterPick(picks, null, "w_", "0", Const.MODULE , "", preUrl + "moduleId/moduleName", "");
				
				pick = new PickDto(Const.SEPARATOR, "前端文档");
				picks.add(pick);
				preUrl = "#/webSource/list/";
				pick = new PickDto("source_0", preUrl + "0/根目录", "根目录");
				picks.add(pick);
				dataCenter.getDataCenterPick(picks, null, "source_", "0", Const.DIRECTORY, "--", preUrl + "moduleId/moduleName", "");
				
				pick = new PickDto(Const.SEPARATOR, "前端数据字典列表");
				picks.add(pick);
				preUrl = "#/webWebPage/list/";
				pick = new PickDto("DICTIONARY", preUrl + "DICTIONARY/null", "数据字典列表");
				picks.add(pick);
				// 分割线
				pick = new PickDto(Const.SEPARATOR, "前端文章类目列表");
				picks.add(pick);
				preUrl = "#/webWebPage/list/ARTICLE/";
				int j = 0;
				@SuppressWarnings("unchecked")
				List<String> categorys2 = (List<String>) webPageService.queryByHql("select distinct category from WebPage", null);
				for (String w : categorys2) {
					if (w == null)
						continue;
					j++;
					pick = new PickDto("cat_" + j, preUrl + w, w);
					picks.add(pick);
				}
				// 分割线
				pick = new PickDto(Const.SEPARATOR, "前端页面");
				picks.add(pick);
				preUrl = "#/webWebPage/detail/PAGE/";
				for (WebPage w : webPageService
						.findByMap(Tools.getMap("key|" + Const.NOT_NULL, Const.NOT_NULL, "type", "PAGE"), null, null)) {
					pick = new PickDto("wp_" + w.getKey(), preUrl + w.getKey(), w.getName());
					picks.add(pick);
				}
				// 分割线
				return;
			}
						
		case "DATACENTER":// 所有数据
			dataCenter.getDataCenterPick(picks, null, "", "0", key,  "" , "", "");
			if(key.equals(Const.DIRECTORY)){
				picks.add(0,new PickDto("0","根目录"));
			}else{
				picks.add(0,new PickDto("0","顶级项目"));
			}
			return;
		case "LEAFMODULE":// 查询叶子模块
			@SuppressWarnings("unchecked")
			List<DataCenter> modules = (List<DataCenter>) dataCenter
					.queryByHql("from Module m where type='MODULE' and m.id not in (select m2.parentId from Module m2)",null);
			for (DataCenter m : modules) {
				pick = new PickDto(m.getId(), m.getName());
				picks.add(pick);
			}
			return;
		case "FONTFAMILY":// 字体
			for (FontFamilyType font : FontFamilyType.values()) {
				pick = new PickDto(font.name(), font.getValue(), font.getName());
				picks.add(pick);
			}
			return;
		case "USERTYPE": // 用户类型
			for (UserType type : UserType.values()) {
				pick = new PickDto("user-type"+type.getName(), type.getName(), type.name());
				picks.add(pick);
			}
			return;
		}
	}

}
