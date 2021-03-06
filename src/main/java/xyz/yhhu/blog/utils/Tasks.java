package xyz.yhhu.blog.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.yhhu.blog.entity.SendComment;
import xyz.yhhu.blog.service.SendCommentService;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Tiger
 * @date 2020-07-19
 * @see xyz.yhhu.blog.utils
 **/
@Component
public class Tasks {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    SendCommentService sendCommentService;
    @Autowired
    EmailTool emailTool;
    @Value("${spring.datasource.username}")
    private String dataBaseUserName;
    @Value("${spring.datasource.password}")
    private String dataBasePassword;
    @Value("${mysql.dump.location}")
    private String savePath;
    @Value("${databaseName}")
    private String dataBaseName;
    @Value("${email.adminEmail}")
    private String to;
    @Scheduled(cron = "0 00 08 * * ? ")
    public void sendEmail() {
        System.out.println("现在是:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ",开始发送邮件");
        StringBuilder sendText = new StringBuilder();
        String blogUrl = "该条博客地址：http://blog.yhhu.xyz/#/blog/";
        List<SendComment> list = sendCommentService.list();
        for (SendComment item : list) {
            sendText.append(item.getReaderName()).append("在《").append(item.getTitle()).append("》下回复----")
                    .append(item.getContent()).append("----").append(blogUrl)
                    .append(item.getBlogId()).append("\n");
            sendCommentService.removeById(item.getId());
        }
        if(list.size()>0) {
            emailTool.sendSimpleMail(to,"Blog每日评论",String.valueOf(sendText));
        }
        System.out.println("今天的邮件已经发送完成了");
    }

    /**
     * 每周备份一次
     * @return void
     **/
    @Scheduled(cron = "0 00 00 ? * MON ")
    public void saveDataBase() throws IOException, MessagingException {
        System.out.println("现在是:" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ",开始备份数据库");
        File file = new File(savePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        String window_cmd = "cmd /c mysqldump -u" + dataBaseUserName + " -p" + dataBasePassword + " vueblog >"
                + savePath + new SimpleDateFormat("yyyy-MM-dd").format(new Date())
                + ".sql";
        String fileUrl=savePath + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".sql";
        String[] centOS_cmd = {"/bin/sh","-c","mysqldump -u" + dataBaseUserName + " -p"
                + dataBasePassword + " " + dataBaseName + " > "
                + fileUrl};
        Process process = Runtime.getRuntime().exec(centOS_cmd);
        System.out.println("开始将数据库发送到管理员邮箱。");
        emailTool.sendAttachmentsMail(to,"数据库备份","当前时间为："+ new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()),fileUrl);
        File file1=new File(fileUrl);
        if(!file1.delete()){
            logger.error("删除备份数据异常");
        }
    }
}
