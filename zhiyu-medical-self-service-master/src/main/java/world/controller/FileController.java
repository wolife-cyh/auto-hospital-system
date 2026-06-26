package world.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import world.component.OssClient;
import world.dto.RespResult;
import world.entity.User;
import world.utils.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件控制器
 *
 */
@RestController
@RequestMapping("/file")
public class FileController extends BaseController<User> {

    @Autowired
    private OssClient ossClient;

    /**
     * 上传文件到 OSS（原有逻辑，用于诊断书等需要云端存储的场景）
     */
    @PostMapping("/upload")
    public RespResult upload(@RequestParam("file") MultipartFile file) throws IOException {
        String url = ossClient.upload(file, String.valueOf(loginUser.getId()));
        if (Assert.isEmpty(url)) {
            return RespResult.fail("上传失败", url);
        }
        return RespResult.success("上传成功", url);
    }

    /**
     * 上传文件到本地存储
     * <p>
     * 文件保存到 ./uploads/illness/ 目录，返回访问 URL。
     * 适用于疾病图片、药品图片等不需要云端存储的场景。
     *
     * @param file 上传的文件
     * @param type 业务类型（如 "illness"、"medicine"），用于子目录隔离
     * @return 文件的访问 URL（如 /uploads/illness/xxx.jpg）
     */
    @PostMapping("/local-upload")
    public RespResult uploadLocal(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "type", defaultValue = "illness") String type) {
        if (file == null || file.isEmpty()) {
            return RespResult.fail("文件不能为空");
        }

        try {
            // 确定上传目录: ./uploads/{type}/
            String uploadDir = "uploads" + File.separator + type;
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名（保留原始扩展名）
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 构造访问 URL
            String url = "/" + uploadDir.replace(File.separator, "/") + "/" + newFilename;
            return RespResult.success("上传成功", url);
        } catch (IOException e) {
            return RespResult.fail("上传失败: " + e.getMessage());
        }
    }
}