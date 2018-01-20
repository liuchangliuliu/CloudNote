package cn.edu.jit.web;


import cn.edu.jit.dto.*;
import cn.edu.jit.entry.*;
import cn.edu.jit.global.GlobalConstant;
import cn.edu.jit.global.GlobalFunction;
import cn.edu.jit.service.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 管理员web层
 * @author jitwxs
 * @date 2018/1/6 9:16
 */
@Controller
@RequestMapping(value = "/admin")
public class AdminController {

    @Resource(name = "logServiceImpl")
    private LogService logService;

    @Resource(name = "userServiceImpl")
    private UserService userService;

    @Resource(name = "articleServiceImpl")
    private ArticleService articleService;

    @Resource(name = "loginServiceImpl")
    private LoginService loginService;

    @Resource(name = "areaServiceImpl")
    private AreaService areaService;

    @Resource(name = "notifyServiceImpl")
    private NotifyService notifyService;

    @Resource(name = "illegalReasonServiceImpl")
    private IllegalReasonService illegalReasonService;

    @Resource(name = "userBlacklistServiceImpl")
    private UserBlacklistService userBlacklistService;

    @Resource(name = "userPanServiceImpl")
    private UserPanService userPanService;

    /*---------   普通方法区域（START）   ----------*/

    private String getSelfId() {
        User user = userService.getByTel(GlobalFunction.getSelfTel());
        return user.getId();
    }

    private List<UserDto> user2Dto(List<User> users) {
        List<UserDto> result = new ArrayList<>();

        for(User user : users) {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(user, userDto);
            if(userDto.getArea() != null) {
                Area area = areaService.getById(user.getArea());
                userDto.setAreaName(area.getName());
            }

            int roleId = loginService.getByTel(user.getTel()).getRoleId();
            if(roleId == GlobalConstant.ROLE.ADMIN.getIndex()) {
                userDto.setRoleName(GlobalConstant.ROLE.ADMIN.getName());
            } else if(roleId == GlobalConstant.ROLE.USER.getIndex()) {
                userDto.setRoleName(GlobalConstant.ROLE.USER.getName());
            }
            result.add(userDto);
        }
        return result;
    }

    private List<ArticleDto> article2Dto(List<Article> articles) {
        List<ArticleDto> lists = new ArrayList<>();

        for (Article article: articles) {
            ArticleDto articleDto = new ArticleDto();
            BeanUtils.copyProperties(article, articleDto);
            User user = userService.getById(article.getUserId());
            articleDto.setAuthorTel(user.getTel());
            articleDto.setAuthorName(user.getName());
            if(!StringUtils.isBlank(article.getShareUrl())) {
                articleDto.setShareUrl(GlobalFunction.getRealUrl(article.getShareUrl()));
            }
            lists.add(articleDto);
        }
        return  lists;
    }

    private List<UserBlacklistDto> userBlacklist2Dto(List<UserBlacklist> userBlacklists) {
        List<UserBlacklistDto> result = new ArrayList<>();

        for(UserBlacklist userBlacklist : userBlacklists) {
            UserBlacklistDto userBlacklistDto = new UserBlacklistDto();
            BeanUtils.copyProperties(userBlacklist, userBlacklistDto);

            String userId = userBlacklist.getUserId();
            if(userId != null) {
                User user = userService.getById(userId);
                IllegalReason illegalReason = illegalReasonService.getById(userBlacklist.getReasonId());

                if(user != null) {
                    userBlacklistDto.setUserName(user.getName());
                    userBlacklistDto.setTel(user.getTel());
                }
                if(illegalReason != null) {
                    userBlacklistDto.setIllegalName(illegalReason.getName());
                }

                if(userBlacklistDto.getEndDate().compareTo(new Date()) <= 0) {
                    userBlacklistDto.setStatus("失效");
                } else {
                    userBlacklistDto.setStatus("有效");
                }
            }
            result.add(userBlacklistDto);
        }
        return result;
    }

    private LogDto log2LogDto(Log log) {
        LogDto logDto = new LogDto();
        BeanUtils.copyProperties(log, logDto);
        User user = userService.getById(log.getUserId());
        if(user != null) {
            logDto.setUserName(user.getName());
        }

        return logDto;
    }

    private List<LogDto> log2LogDto(List<Log> list) {
        List<LogDto> result = new ArrayList<>();
        for(Log log : list) {
            LogDto logDto = new LogDto();
            BeanUtils.copyProperties(log, logDto);
            User user = userService.getById(log.getUserId());
            if(user != null) {
                logDto.setUserName(user.getName());
            }
            result.add(logDto);
        }
        return result;
    }

    /*---------   普通方法区域（END）   ----------*/

    /*---------   网站信息区域（Start）   ----------*/

    @RequestMapping(value = "index")
    public String index() {
        return "admin/userInfo";
    }

    /**
     * 用户信息UI
     */
    @RequestMapping(value = "userInfo", method = {RequestMethod.GET})
    public String userInfoUI() {
        return "/admin/userInfo";
    }

    /**
     * 准备用户信息数据
     */
    @RequestMapping(value = "preparerUserInfo", method = {RequestMethod.GET})
    public void preparerUserInfo(HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        try {
            Map<Object, Object> objects = new HashMap<>(16);

            List<User> users = userService.listAllUser("create_date");

            List<UserInfo> regInfo = new ArrayList<>();

            // 准备数据
            User endUser = users.get(users.size() - 1);
            Date endDate = endUser.getCreateDate();
            UserInfo userInfo = new UserInfo();
            userInfo.setDate(GlobalFunction.getDate2Day(endDate));
            userInfo.setTempTotal(users.size());
            String sex = endUser.getSex();
            if("男".equals(sex)) {
                userInfo.setMaleNum(userInfo.getMaleNum() + 1);
            } else if("女".equals(sex)) {
                userInfo.setFemaleNum(userInfo.getFemaleNum() + 1);
            }

            for(int i=users.size()-2; i>=0; i--) {
                User nowUser = users.get(i);
                User prevUser = users.get(i + 1);

                Date nowDate = nowUser.getCreateDate();
                Date prevDate = prevUser.getCreateDate();

                if(!DateUtils.isSameDay(nowDate, prevDate)) {
                    userInfo.setNum(userInfo.getMaleNum() + userInfo.getFemaleNum());
                    regInfo.add(userInfo);
                    userInfo = new UserInfo();
                    userInfo.setDate(GlobalFunction.getDate2Day(nowDate));
                    userInfo.setTempTotal(i + 1);
                }
                String nowSex = nowUser.getSex();
                if("男".equals(nowSex)) {
                    userInfo.setMaleNum(userInfo.getMaleNum() + 1);
                } else if("女".equals(nowSex)) {
                    userInfo.setFemaleNum(userInfo.getFemaleNum() + 1);
                }
            }
            regInfo.add(userInfo);

            // 拼接JSON串
            int maleCount = userService.countBySex("男");
            int femaleCount = userService.countBySex("女");

            objects.put("regInfo", regInfo);
            objects.put("maleCount", maleCount);
            objects.put("femaleCount", femaleCount);

            String data = JSON.toJSONString(objects, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 登陆信息UI
     */
    @RequestMapping(value = "loginInfo", method = {RequestMethod.GET})
    public String loginInfoUI() {
        return "/admin/loginInfo";
    }

    /**
     * 准备登陆信息数据
     */
    @RequestMapping(value = "preparerLoginInfo", method = {RequestMethod.GET})
    public void preparerLoginInfo(HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        try {
            List<Data> list = logService.countUserByTitle(GlobalConstant.LOG_USER.USER_LOGIN.getName());
            String data = JSON.toJSONString(list, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 分享信息UI
     */
    @RequestMapping(value = "shareInfo", method = {RequestMethod.GET})
    public String shareInfoUI() {
        return "/admin/shareInfo";
    }

    /**
     * 准备分享信息数据
     */
    @RequestMapping(value = "preparerShareInfo", method = {RequestMethod.GET})
    public void preparerShareInfo(HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        try {
            List<Data> list = logService.countUserByTitle(GlobalConstant.LOG_NOTE.SHARE_NOTE.getName());
            String data = JSON.toJSONString(list, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*---------   网站信息区域（End）   ----------*/

    /*---------   笔记管理区域（Start）   ----------*/
    /**
     * 笔记日志UI
     */
    @RequestMapping(value = "noteLog", method = {RequestMethod.GET})
    public String noteLogUI() {
        return "admin/noteLog";
    }

    /**
     * 准备笔记日志
     */
    @RequestMapping(value = "prepareNoteLog", method = {RequestMethod.GET})
    public void prepareNoteLog(HttpServletResponse response) {
        try {
            response.setContentType("text/html;charset=utf-8");
            List<Log> lists = logService.listByType(GlobalConstant.LOG_NOTE.type, "create_date desc");

            List<LogDto> result = log2LogDto(lists);
            String data = JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 分享审核UI
     */
    @RequestMapping(value = "shareAudit", method = {RequestMethod.GET})
    public String shareAuditUI() {
        return "admin/shareAudit";
    }

    /**
     * 准备分享审核数据
     */
    @RequestMapping(value = "prepareShareAudit", method = {RequestMethod.GET})
    public void prepareShareAudit(HttpServletResponse response) {
        try {
            response.setContentType("text/html;charset=utf-8");
            List<Article> tempList = articleService.listAllShareArticle();
            List<ArticleDto> list = article2Dto(tempList);
            String data = JSON.toJSONString(list, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消分享
     */
    @RequestMapping(value = "cancelShare", method = {RequestMethod.GET})
    public String cancelShare(String id, HttpServletRequest request) {
        Article article = articleService.getById(id);
        article.setIsOpen(GlobalConstant.NOTE_STATUS.NOT_SHARE.getIndex());
        if(!StringUtils.isBlank(article.getShareUrl())) {
            GlobalFunction.deleteSignalFile(article.getShareUrl());
            article.setShareUrl(null);
        }
        articleService.update(article);

        // 保存日志
        logService.saveLog(request, GlobalConstant.LOG_SYSTEM.type, GlobalConstant.LOG_SYSTEM.SHARE_CANCEL.getName(), getSelfId());

        // 发送消息
        String sendId = getSelfId();
        Notify notify = new Notify();
        notify.setId(GlobalFunction.getUUID());
        notify.setType(GlobalConstant.NOTIFY.NOTIFY_NOTE.getName());
        notify.setSendId(sendId);
        notify.setRecvId(article.getUserId());
        notify.setStatus(GlobalConstant.NOTIFY_STATUS.UNREAD.getIndex());
        notify.setTitle("分享笔记被取消");
        notify.setContent("您的笔记文章《" + article.getTitle() +"》已被系统取消分享");
        notify.setCreateDate(new Date());
        notifyService.save(notify);

        // 保存日志
        logService.saveLog(request, GlobalConstant.NOTIFY.type, GlobalConstant.NOTIFY.NOTIFY_NOTE.getName(), sendId);

        return "redirect:/admin/shareAudit";
    }

    /**
     * 删除文章，管理员删除的文章，不进入回收站
     */
    @RequestMapping(value = "deleteArticle", method = {RequestMethod.GET})
    public String deleteArticle(String[] ids, HttpServletRequest request) {
        for(String id : ids) {
            if(!StringUtils.isBlank(id)) {
                articleService.removeById(id);
                // 保存日志
                logService.saveLog(request, GlobalConstant.LOG_SYSTEM.type, GlobalConstant.LOG_SYSTEM.SHARE_DEL.getName(), getSelfId());

                // 发送消息
                Article article = articleService.getById(id);
                String sendId = getSelfId();
                Notify notify = new Notify();
                notify.setId(GlobalFunction.getUUID());
                notify.setType(GlobalConstant.NOTIFY.NOTIFY_NOTE.getName());
                notify.setSendId(sendId);
                notify.setRecvId(article.getUserId());
                notify.setStatus(GlobalConstant.NOTIFY_STATUS.UNREAD.getIndex());
                notify.setTitle("分享笔记被删除");
                notify.setContent("您的笔记文章《" + article.getTitle() +"》已被系统删除");
                notify.setCreateDate(new Date());
                notifyService.save(notify);

                // 保存日志
                logService.saveLog(request, GlobalConstant.NOTIFY.type, GlobalConstant.NOTIFY.NOTIFY_NOTE.getName(), sendId);
            }
        }
        return "redirect:/admin/shareAudit";
    }

    /*---------   笔记管理区域（End）   ----------*/

    /*---------   用户管理区域（Start）   ----------*/
    /**
     *  用户列表UI
     */
    @RequestMapping(value = "userList", method = {RequestMethod.GET})
    public String userListUI() {
        return "admin/userList";
    }

    /**
     * 准备用户列表
     */
    @RequestMapping(value = "prepareUserList", method = {RequestMethod.GET})
    public void prepareUserList(HttpServletResponse response) {
        try {
            response.setContentType("text/html;charset=utf-8");
            List<User> list = userService.listAllUser("create_date");
            List<UserDto> userDtos = user2Dto(list);
            String data = JSON.toJSONString(userDtos, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 用户日志UI
     */
    @RequestMapping(value = "userLog", method = {RequestMethod.GET})
    public String userLogUI() {
        return "/admin/userLog";
    }

    /**
     * 准备用户日志
     */
    @RequestMapping(value = "prepareUserLog", method = {RequestMethod.GET})
    public void prepareUserLog(HttpServletResponse response) {
        try {
            response.setContentType("text/html;charset=utf-8");
            List<Log> lists = logService.listByType(GlobalConstant.LOG_USER.type, "create_date desc");

            List<LogDto> result = log2LogDto(lists);
            String data = JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除用户
     */
    @RequestMapping(value = "deleteUser", method = {RequestMethod.GET})
    public String deleteUser(String[] tels, HttpServletRequest request) {
        for(String tel : tels) {
            // 无需删除User表，级联于Login表
            loginService.removeByTel(tel);

            // 安全删除文件夹
            File file = new File(GlobalConstant.UPLOAD_PATH + "/" + tel);
            FileUtils.deleteQuietly(file);
        }

        // 保存日志
        logService.saveLog(request, GlobalConstant.LOG_SYSTEM.type, GlobalConstant.LOG_SYSTEM.USER_DEL.getName(), getSelfId());
        return "redirect:/admin/userList";
    }

    /**
     * 显示用户信息
     */
    @RequestMapping(value = "showUserInfo", method = {RequestMethod.POST})
    public void showUserInfo(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        String tel = request.getParameter("tel");
        try {
            User user = userService.getByTel(tel);
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(user, userDto);

            if(userDto.getArea() != null) {
                Area area = areaService.getById(user.getArea());
                userDto.setAreaName(area.getName());
            }

            String data = JSON.toJSONString(userDto, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打入小黑屋前验证
     */
    @RequestMapping(value = "addBlacklistCheck", method = {RequestMethod.POST})
    public void addBlacklistCheck(HttpServletRequest request, HttpServletResponse response) {
        Message message = new Message();
        response.setContentType("text/html;charset=utf-8");
        // 如果已经存在有效的封禁记录，提示管理员到期时间，以及是否要继续封禁
        String tel = request.getParameter("tel");
        try {
            User user = userService.getByTel(tel);
            if(!StringUtils.isBlank(tel)) {
                List<UserBlacklist> list = userBlacklistService.listValid(user.getId());
                if(list.size() > 0) {
                    Date maxDate = new Date();
                    for(UserBlacklist userBlacklist : list) {
                        Date tempDate = userBlacklist.getEndDate();
                        if(tempDate.compareTo(maxDate) >= 0) {
                            maxDate = tempDate;
                        }
                    }
                    message.setStatus(true);
                    String info = "共计查到" + list.size() + "条有效记录，最后解封时间为："
                            + GlobalFunction.getDate2Second(maxDate) +"，是否继续添加封禁记录？";
                    message.setInfo(info);
                } else {
                    message.setStatus(false);
                }
            }
            String data = JSON.toJSONString(message, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打入小黑屋
     */
    @RequestMapping(value = "addToBlackHome", method = {RequestMethod.POST})
    public String addToBlacklist(UserBlacklist userBlacklist, Integer duration, HttpServletRequest request) {
        try {
            userBlacklist.setId(GlobalFunction.getUUID());
            Date d = new Date();
            userBlacklist.setCreateDate(d);
            if(duration == 9999) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                userBlacklist.setEndDate(df.parse("9999-12-31 00:00"));
            } else {
                userBlacklist.setEndDate(new Date(d.getTime() + duration * 24 * 60 * 60 * 1000));
            }
            userBlacklistService.save(userBlacklist);

            // 保存日志
            logService.saveLog(request, GlobalConstant.LOG_SYSTEM.type, GlobalConstant.LOG_SYSTEM.USER_BLOCK.getName(), getSelfId());

            // 发送消息
            IllegalReason illegalReason = illegalReasonService.getById(userBlacklist.getReasonId());
            String reason = illegalReason.getName();
            String sendId = getSelfId();
            Notify notify = new Notify();
            notify.setId(GlobalFunction.getUUID());
            notify.setType(GlobalConstant.NOTIFY.NOTIFY_SYSTEM.getName());
            notify.setSendId(sendId);
            notify.setRecvId(userBlacklist.getUserId());
            notify.setStatus(GlobalConstant.NOTIFY_STATUS.UNREAD.getIndex());
            notify.setTitle("账户被封禁");
            notify.setContent("您的账户已被封禁，封禁原因：“" + reason +"”，解封时间为：" + GlobalFunction.getDate2Second(userBlacklist.getEndDate()));
            notify.setCreateDate(new Date());
            notifyService.save(notify);

            // 保存日志
            logService.saveLog(request, GlobalConstant.NOTIFY.type, GlobalConstant.NOTIFY.NOTIFY_SYSTEM.getName(), sendId);
        } catch (ParseException e) {
            e.printStackTrace();
            // 保存日志
            logService.saveLog(request, e, GlobalConstant.LOG_SYSTEM.type, GlobalConstant.LOG_SYSTEM.USER_BLOCK.getName(), getSelfId());
        }
        return "redirect:/admin/userList";
    }

    /**
     *  小黑屋UI
     */
    @RequestMapping(value = "blackHome", method = {RequestMethod.GET})
    public String blackHomeUI() {
        return "admin/blackHome";
    }

    /**
     * 准备小黑屋数据
     */
    @RequestMapping(value = "prepareBlackHome", method = {RequestMethod.GET})
    public void prepareBlackHome(HttpServletResponse response) {
        try {
            response.setContentType("text/html;charset=utf-8");
            List<UserBlacklist> tempList = userBlacklistService.listAll("create_date");

            List<UserBlacklistDto> list = userBlacklist2Dto(tempList);

            String data = JSON.toJSONString(list, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消黑名单
     */
    @RequestMapping(value = "cancelBlacklist", method = {RequestMethod.GET})
    public String cancelBlacklist(String id, HttpServletRequest request) {
        UserBlacklist userBlacklist = userBlacklistService.getById(id);
        userBlacklist.setEndDate(new Date());
        userBlacklistService.update(userBlacklist);
        // 保存日志
        logService.saveLog(request, GlobalConstant.LOG_SYSTEM.type, GlobalConstant.LOG_SYSTEM.CANCEL_BLOCK.getName(), getSelfId());

        // 发送消息
        String sendId = getSelfId();
        Notify notify = new Notify();
        notify.setId(GlobalFunction.getUUID());
        notify.setType(GlobalConstant.NOTIFY.NOTIFY_SYSTEM.getName());
        notify.setSendId(sendId);
        notify.setRecvId(userBlacklist.getUserId());
        notify.setStatus(GlobalConstant.NOTIFY_STATUS.UNREAD.getIndex());
        notify.setTitle("账户被解封");
        notify.setContent("您的账户已被解封，现在可以正常登陆了，以后可不要再干违规的事哦！");
        notify.setCreateDate(new Date());
        notifyService.save(notify);
        // 保存日志
        logService.saveLog(request, GlobalConstant.NOTIFY.type, GlobalConstant.NOTIFY.NOTIFY_SYSTEM.getName(), sendId);

        return "redirect:/admin/blackHome";
    }

    /**
     * 删除封禁记录
     */
    @RequestMapping(value = "removeBlacklistRecord", method = {RequestMethod.GET})
    public String removeBlacklistRecord(String[] ids) {
        for(String id : ids) {
            if(!StringUtils.isBlank(id)) {
                userBlacklistService.removeById(id);
            }
        }
        return "redirect:/admin/blackHome";
    }

    /*---------   用户管理区域（End）   ----------*/

    /*---------   网盘管理区域（Start）   ----------*/
    /**
     * 网盘信息UI
     */
    @RequestMapping(value = "panInfo", method = {RequestMethod.GET})
    public String panInfoUI() {
        return "/admin/panInfo";
    }

    /**
     * 准备网盘信息
     */
    @RequestMapping(value = "preparePanInfo", method = {RequestMethod.GET})
    public void preparePanInfo(HttpServletResponse response) {
        try {
            response.setContentType("text/html;charset=utf-8");
            Map<String, Object> maps = new HashMap<>(16);

            // 获取网盘容量百分比
            List<User> users = userService.listAllUser(null);
            int totalUsed = userPanService.countTotalUsedSize();

            String temp = String.format("%.2f", (double) totalUsed / (users.size() * GlobalConstant.DEFAULT_PAN_SIZE));
            double perfect = Double.parseDouble(temp) * 100;

            int[] countUser = new int[10];
            // 获取使用容量每10%的人数
            for(User user : users) {
                Integer used = userPanService.countUsedSize(user.getId());
                if(used != null) {
                    int per = (int)((double)used / GlobalConstant.DEFAULT_PAN_SIZE * 10);
                    countUser[per]++;
                } else {
                    countUser[0]++;
                }
            }

            List<Object> lists = new ArrayList<>();

            for(int i=0; i<countUser.length; i++) {
                Data data = new Data();
                data.setK(i*10 +"% - " + (i+1)*10 + "%");
                data.setV(countUser[i]);
                lists.add(data);
            }

            maps.put("perfect", perfect);
            maps.put("lists", lists);
            String data = JSON.toJSONString(maps, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 网盘日志UI
     */
    @RequestMapping(value = "panLog", method = {RequestMethod.GET})
    public String panLogUI(Model model) {
        List<Log> lists = logService.listByType(GlobalConstant.LOG_PAN.type, "create_date desc");
        model.addAttribute("lists", lists);
        return "/admin/panLog";
    }

    /**
     * 准备用户日志
     */
    @RequestMapping(value = "preparePanLog", method = {RequestMethod.GET})
    public void preparePanLog(HttpServletResponse response) {
        try {
            response.setContentType("text/html;charset=utf-8");
            List<Log> lists = logService.listByType(GlobalConstant.LOG_PAN.type, "create_date desc");

            List<LogDto> result = log2LogDto(lists);
            String data = JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*---------   网盘管理区域（End）   ----------*/


    /*---------   系统管理区域（Start）   ----------*/
    /**
     * 系统设置UI
     */
    @RequestMapping(value = "systemSetting", method = {RequestMethod.GET})
    public String systemSettingUI(Model model) {
        List<IllegalReason> list = illegalReasonService.listAll();
        model.addAttribute("IllegalReasonList", list);
        return "admin/systemSetting";
    }

    /**
     * 系统日志UI
     */
    @RequestMapping(value = "systemLog", method = {RequestMethod.GET})
    public String systemLogUI() {
        return "admin/systemLog";
    }

    /**
     * 准备系统日志
     */
    @RequestMapping(value = "prepareSystemLog", method = {RequestMethod.GET})
    public void prepareSystemLog(HttpServletResponse response) {
        try {
            response.setContentType("text/html;charset=utf-8");
            List<Log> lists = logService.listByType(GlobalConstant.LOG_SYSTEM.type, "create_date desc");

            List<LogDto> result = log2LogDto(lists);
            String data = JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取日志
     */
    @RequestMapping(value = "getLogInfo", method = {RequestMethod.POST})
    public void getLogInfo(HttpServletRequest request, HttpServletResponse response) {
        String id = request.getParameter("id");
        response.setContentType("text/html;charset=utf-8");
        try {
            if(!StringUtils.isBlank(id)) {
                Log log = logService.getById(id);
                LogDto logDto = log2LogDto(log);
                String data = JSON.toJSONString(logDto, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
                response.getWriter().write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除日志
     */
    @RequestMapping(value = "deleteLog", method = {RequestMethod.GET})
    public String deleteLog(String[] logIds, String url) {
        for(String id : logIds) {
            logService.removeById(id);
        }
        return "redirect:/admin/" + url;
    }

    /**
     * 删除封禁原因
     */
    @RequestMapping(value = "deleteIllegal", method = {RequestMethod.GET})
    public String deleteIllegal(String id) {
        illegalReasonService.removeById(id);
        return "redirect:/admin/systemSetting";
    }

    /**
     * 修改封禁原因
     * */
    @RequestMapping(value = "updateIllegal", method = {RequestMethod.POST})
    public String updateIllegal(IllegalReason illegalReason) {
        illegalReasonService.update(illegalReason);
        return "redirect:/admin/systemSetting";
    }

    /**
     * 添加封禁原因
     * */
    @RequestMapping(value = "addIllegal", method = {RequestMethod.POST})
    public String addIllegal(IllegalReason illegalReason) {
        illegalReason.setId(GlobalFunction.getUUID());
        illegalReasonService.save(illegalReason);
        return "redirect:/admin/systemSetting";
    }

    /**
     * 获取所有封禁原因
     */
    @RequestMapping(value = "listIllegal", method = {RequestMethod.GET})
    public void listIllegal(HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        try {
            List<IllegalReason> list = illegalReasonService.listAll();
            String data = JSON.toJSONString(list, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*---------   系统管理区域（End）   ----------*/

    /*---------   消息管理区域（Start）   ----------*/
    /**
     * 消息日志UI
     */
    @RequestMapping(value = "notifyLog", method = {RequestMethod.GET})
    public String notifyLogUI() {
        return "admin/notifyLog";
    }

    /**
     * 准备消息日志
     */
    @RequestMapping(value = "prepareNotifyLog", method = {RequestMethod.GET})
    public void prepareNotifyLog(HttpServletResponse response) {
        try {
            response.setContentType("text/html;charset=utf-8");
            List<Log> lists = logService.listByType(GlobalConstant.NOTIFY.type, "create_date desc");

            List<LogDto> result = log2LogDto(lists);
            String data = JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat);
            response.getWriter().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*---------   消息管理区域（End）   ----------*/
}