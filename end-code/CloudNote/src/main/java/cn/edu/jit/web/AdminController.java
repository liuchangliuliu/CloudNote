package cn.edu.jit.web;

import cn.edu.jit.dto.ArticleDto;
import cn.edu.jit.dto.ArticleRecycleDto;
import cn.edu.jit.dto.UserDto;
import cn.edu.jit.entry.*;
import cn.edu.jit.global.GlobalConstant;
import cn.edu.jit.global.GlobalFunction;
import cn.edu.jit.service.*;
import cn.edu.jit.util.Sha1Utils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sun.deploy.security.MSCryptoDSASignature;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * 管理员web层
 * @author jitwxs
 * @date 2018/1/6 9:16
 */
@Controller
@RequestMapping(value = "/admin")
public class AdminController {

    @Resource(name = "userServiceImpl")
    private UserService userService;

    @Resource(name = "articleServiceImpl")
    private ArticleService articleService;

    @Resource(name = "loginServiceImpl")
    private LoginService loginService;

    @Resource(name = "areaServiceImpl")
    private AreaService areaService;

    @Resource(name = "articleRecycleServiceImpl")
    private ArticleRecycleService articleRecycleService;

    /*---------   普通方法区域（START）   ----------*/
    /**
     * 获取当前用户id
     */
    private String getSelfId() {
        User user = userService.getByTel(GlobalFunction.getSelfTel());
        return user.getId();
    }

    private List<ArticleDto> article2ArticleDto(List<Article> articles) {
        List<ArticleDto> lists = new ArrayList<>();

        for (Article article: articles) {
            ArticleDto articleDto = new ArticleDto();
            BeanUtils.copyProperties(article, articleDto);
            User user = userService.getById(article.getUserId());
            articleDto.setAuthorTel(user.getTel());
            lists.add(articleDto);
        }
        return  lists;
    }

    private List<ArticleRecycleDto> articleRecycle2ArticleRecycleDto(List<ArticleRecycle> articlesRecycle) {
        List<ArticleRecycleDto> lists = new ArrayList<>();

        for (ArticleRecycle articleRecycle: articlesRecycle) {
            ArticleRecycleDto articleRecycleDto = new ArticleRecycleDto();
            BeanUtils.copyProperties(articleRecycle, articleRecycleDto);
            User user = userService.getById(articleRecycle.getUserId());
            articleRecycleDto.setAuthorTel(user.getTel());
            lists.add(articleRecycleDto);
        }
        return  lists;
    }

    /*---------   普通方法区域（END）   ----------*/

    @RequestMapping(value = "index")
    public String index(HttpServletRequest request, HttpServletResponse response) {
        String data = GlobalFunction.getDate();
        Cookie cookie = new Cookie("lastLoginTime", data);
        // 登录时间保存30天
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);

        Cookie[] cookies = request.getCookies();
        String result = "";
        if(cookies.length > 0) {
            for (Cookie ck: cookies) {
                if("lastLoginTime".equals(ck.getName())) {
                    String lastLoginTime = ck.getValue();
                    result = "上次登陆：" + lastLoginTime.replace('#',' ');
                }
            }
        } else {
            result = "上次登陆：未知";
        }
        // 是否显示登陆信息
        if(GlobalConstant.HAS_SHOW_LOGIN_INFO) {
            request.setAttribute("lastLoginTime", result);
            GlobalConstant.HAS_SHOW_LOGIN_INFO = false;
        }
        return "admin/left";
    }


    /*---------   用户管理区域（Start）   ----------*/
    /**
     *  显示所有普通用户
     */
    @RequestMapping(value = "showAllUser", method = {RequestMethod.GET})
    public String showAllUser(Model model) {
        List<User> list = userService.listAllUser();
        model.addAttribute("userList", list);
        return "admin/showAllUser";
    }

    /**
     * 获取指定用户所有文章
     */
    @RequestMapping(value = "showUserArticle", method = {RequestMethod.POST})
    public void showUserArticle(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        try {
            String userId = request.getParameter("id");
            List<Article> articles = articleService.listArticleByUid(userId);
            Message message = new Message();
            message.setArticles(articles);
            String data = JSON.toJSONString(message, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  显示所有分享笔记
     */
    @RequestMapping(value = "showShareNote", method = {RequestMethod.GET})
    public String showShareNote(Model model) {
        List<Article> tempList = articleService.listAllShareArticle();
        List<ArticleDto> list = article2ArticleDto(tempList);
        model.addAttribute("noteList", list);
        return "admin/showShareNote";
    }

    /**
     *  显示所有回收笔记
     */
    @RequestMapping(value = "showNoteRecycle", method = {RequestMethod.GET})
    public String showNoteRecycle(Model model) {
        List<ArticleRecycle> tempList = articleRecycleService.listAllRecycle();
        List<ArticleRecycleDto> list = articleRecycle2ArticleRecycleDto(tempList);
        model.addAttribute("recycleList", list);
        return "admin/showNoteRecycle";
    }

    /**
     *  显示所有笔记
     */
    @RequestMapping(value = "showAllNote", method = {RequestMethod.GET})
    public String showAllNote(Model model) {
        List<Article> tempList = articleService.listAllArticle();
        List<ArticleDto> list = article2ArticleDto(tempList);
        model.addAttribute("noteList", list);
        return "admin/showAllNote";
    }

    /**
     * 删除文章
     */
    @RequestMapping(value = "deleteArticle", method = {RequestMethod.GET})
    public String deleteArticle(String id) {
        ArticleRecycle articleRecycle = new ArticleRecycle();
        Article article = articleService.getById(id);
        BeanUtils.copyProperties(article, articleRecycle);
        articleService.removeById(id);
        articleRecycleService.save(articleRecycle);
        return "redirect:/admin/showAllNote";
    }

    /**
     * 取消分享
     */
    @RequestMapping(value = "cancelShare", method = {RequestMethod.GET})
    public String cancelShare(String id) {
        Article article = articleService.getById(id);
        article.setIsOpen(GlobalConstant.ARTICLE_STATUS.NOT_SHARE.getIndex());
        article.setModifedDate(new Date());
        articleService.update(article);
        return "redirect:/admin/showShareNote";
    }

    /**
     * 还原文章
     */
    @RequestMapping(value = "restoreArticle", method = {RequestMethod.GET})
    public String restoreArticle(String id) {
        Article article = new Article();
        ArticleRecycle articleRecycle = articleRecycleService.getById(id);
        BeanUtils.copyProperties(articleRecycle, article);
        articleRecycleService.removeById(id);
        articleService.save(article);
        return "redirect:/admin/showNoteRecycle";
    }

    /**
     * 删除用户
     */
    @RequestMapping(value = "deleteUser", method = {RequestMethod.GET})
    public String deleteUser(String tel) throws Exception{
        Login login = loginService.getByTel(tel);
        if (login == null) {
            throw new Exception("用户不存在!");
        } else {
            //login表和user表中删除此用户
            loginService.removeByTel(tel);
            userService.removeByTel(tel);
            //删除此用户的文件
            try {
                GlobalFunction.delFolder(GlobalConstant.UPLOAD_PATH + "/" + tel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "redirect:/admin/showAllUser";
    }

    /**
     * 跳转添加用户
     */
    @RequestMapping(value = "addNewUser", method = {RequestMethod.GET})
    public String addNewUser() {
        return "admin/addNewUser";
    }

    /**
     * 添加用户
     */
    @RequestMapping(value = "addUser", method = {RequestMethod.POST})
    public void addUser(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        String tel = request.getParameter("tel");
        String password = request.getParameter("password");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
//        String area = request.getParameter("area");
        String sex = request.getParameter("sex");
        String sign = request.getParameter("sign");

        try {
            Login checkLogin = loginService.getByTel(tel);
            Message message = new Message();
            if (checkLogin != null) {
                message.setStatus(false);
                message.setInfo("用户已存在");
            } else {
                Login login = new Login();
                User user = new User();
                login.setCreateDate(new Date());
                String encryptedPassword = Sha1Utils.entryptPassword(password);
                login.setPassword(encryptedPassword);
                login.setTel(tel);
                loginService.save(login);
//                user.setArea(area);
                user.setCreateDate(new Date());
                user.setEmail(email);
                user.setId(GlobalFunction.getUUID());
                user.setName(name);
                user.setSex(sex);
                user.setSign(sign);
                userService.save(user);
                message.setStatus(true);
                message.setInfo("添加成功");
            }
            String data = JSON.toJSONString(message, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示用户信息
     */
    @RequestMapping(value = "showUserInfo", method = {RequestMethod.GET})
    public void showUserInfo(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        String tel = request.getParameter("tel");
        try {
            User user = userService.getByTel(tel);
            Area area = areaService.getById(user.getArea());
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(user, userDto);
            userDto.setAreaName(area.getName());

            List<Area> areas = areaService.listByPid(0);

            List<Object> objects = new LinkedList<>();
            objects.add(userDto);
            objects.add(areas);
            String data = JSON.toJSONString(objects, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 修改用户信息
     * */
    @RequestMapping(value = "saveUserInfo", method = {RequestMethod.POST})
    public String saveUserInfo(User user, HttpServletRequest request) {
        String id = user.getId();
        User tempUser = userService.getById(id);
        user.setCreateDate(tempUser.getCreateDate());
        if (user.getArea() == -1) {
            user.setArea(tempUser.getArea());
        }
        user.setModifedDate(new Date());
        userService.update(user);
        return "redirect:/admin/showAllUser";
    }

    /**
     * 修改用户密码
     * */
    @RequestMapping(value = "saveUserLogin", method = {RequestMethod.POST})
    public String saveUserLogin(Login templogin, HttpServletRequest request) {
        String tel = templogin.getTel();
        String password = templogin.getPassword();
        String encryptedPassword = Sha1Utils.entryptPassword(password);
        Login login = loginService.getByTel(tel);
        login.setPassword(encryptedPassword);
        login.setModifiedDate(new Date());
        loginService.update(login);
        return "redirect:/admin/showAllUser";
    }

    /**
     * 验证密码
     */
    @RequestMapping(value = "verifyPassword", method = {RequestMethod.POST})
    public void resetPassword(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        try {
            String passWord = request.getParameter("password");
            Login login = loginService.getByTel(GlobalFunction.getSelfTel());
            Message msg = new Message();
            if (!Sha1Utils.validatePassword(passWord, login.getPassword())) {
                msg.setStatus(false);
                msg.setInfo("密码错误");
            } else {
                msg.setStatus(true);
                msg.setInfo("验证成功");
            }
            String data = JSON.toJSONString(msg, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重置密码
     */
    @RequestMapping(value = "resetPassword", method = {RequestMethod.POST})
    public String resetPassword(String newPassword) {
        Login login = loginService.getByTel(GlobalFunction.getSelfTel());
        String encryptedPassword = Sha1Utils.entryptPassword(newPassword);
        login.setPassword(encryptedPassword);
        login.setModifiedDate(new Date());
        loginService.update(login);
        return "redirect:/logout";
    }
    /*---------   用户管理区域（END）   ----------*/
}
