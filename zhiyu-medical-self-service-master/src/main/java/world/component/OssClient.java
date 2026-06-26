package world.component;

import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
@Slf4j
@Component
public class OssClient {

    @Value("${oss.bucket-name}")
    private String bucketName;

    @Value("${oss.end-point}")
    private String endPoint;

    @Value("${oss.access-key}")
    private String accessKeyId;

    @Value("${oss.access-secret}")
    private String accessKeySecret;

    /**
     * 上传文件（含用户隔离路径）
     * @param file   文件
     * @param path   业务前缀，如 "medical_records"
     * @param userId 用户ID，用于路径隔离
     */
    public String upload(MultipartFile file, String path, Integer userId) throws IOException {
        return doUpload(file, path, userId);
    }

    /**
     * 上传文件（无用户隔离，向后兼容）
     */
    public String upload(MultipartFile file, String path) throws IOException {
        return doUpload(file, path, null);
    }

    private String doUpload(MultipartFile file, String path, Integer userId) throws IOException {
        if (file == null || path == null) {
            return null;
        }

        OSS ossClient = new OSSClientBuilder().build(endPoint, accessKeyId, accessKeySecret);

        try {
            // 检查bucket是否存在
            if (!ossClient.doesBucketExist(bucketName)) {
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
                ossClient.createBucket(createBucketRequest);
            }

            String extension = getFileExtension(file);
            // 文件路径格式: {业务前缀}/{用户ID(可选)}/{UUID}.{ext}
            String fileUrl = path + "/";
            if (userId != null) {
                fileUrl += userId + "/";
            }
            fileUrl += IdUtil.simpleUUID() + extension;

            // 上传文件到OSS
            ossClient.putObject(new PutObjectRequest(bucketName, fileUrl, file.getInputStream()));

            // 生成签名URL（有效期30分钟）
            Date expiration = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileUrl);
            generatePresignedUrlRequest.setExpiration(expiration);
            URL signedUrl = ossClient.generatePresignedUrl(generatePresignedUrlRequest);

            String urlStr = signedUrl.toString();
            log.info("生成签名URL成功: {}", urlStr);

            return urlStr;
        } finally {
            ossClient.shutdown();
        }
    }

    public static String getFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return ".unknown";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
