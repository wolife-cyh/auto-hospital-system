package world.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时自动执行数据库迁移（兼容 MySQL 5.7）
 */
@Slf4j
@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // 检查 img_path 列是否存在
            String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = (SELECT DATABASE()) AND TABLE_NAME = 'illness' AND COLUMN_NAME = 'img_path'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (count == null || count == 0) {
                // 列不存在，添加
                jdbcTemplate.execute("ALTER TABLE `illness` ADD COLUMN `img_path` varchar(255) DEFAULT NULL COMMENT '疾病图片'");
                log.info("✅ 已添加 illness.img_path 字段");
            } else {
                log.info("ℹ️ illness.img_path 字段已存在，跳过");
            }

            // 更新湿疹的图片为本地图片
            int updated = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/eczema.webp",
                "湿疹"
            );
            if (updated > 0) {
                log.info("✅ 已更新湿疹的疾病图片为本地图片");
            }

            // 更新病毒性感冒的图片为本地图片
            int updatedCold = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/cold.webp",
                "病毒性感冒"
            );
            if (updatedCold > 0) {
                log.info("✅ 已更新病毒性感冒的疾病图片为本地图片");
            }

            // 更新风寒感冒的图片为本地图片
            int updatedWindCold = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/cold-wind.jpg",
                "风寒感冒"
            );
            if (updatedWindCold > 0) {
                log.info("✅ 已更新风寒感冒的疾病图片为本地图片");
            }

            // 更新扁桃体发炎的图片为本地图片
            int updatedTonsillitis = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/tonsillitis.webp",
                "扁桃体发炎"
            );
            if (updatedTonsillitis > 0) {
                log.info("✅ 已更新扁桃体发炎的疾病图片为本地图片");
            }

            // 更新偏头痛的图片为本地图片
            int updatedMigraine = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/migraine.webp",
                "偏头痛"
            );
            if (updatedMigraine > 0) {
                log.info("✅ 已更新偏头痛的疾病图片为本地图片");
            }

            // 更新便秘的图片为本地图片
            int updatedConstipation = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/constipation.webp",
                "便秘"
            );
            if (updatedConstipation > 0) {
                log.info("✅ 已更新便秘的疾病图片为本地图片");
            }

            // 更新骨折的图片为本地图片
            int updatedFracture = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/fracture.webp",
                "骨折"
            );
            if (updatedFracture > 0) {
                log.info("✅ 已更新骨折的疾病图片为本地图片");
            }

            // 更新牙周炎的图片为本地图片
            int updatedPeriodontitis = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/periodontitis.webp",
                "牙周炎"
            );
            if (updatedPeriodontitis > 0) {
                log.info("✅ 已更新牙周炎的疾病图片为本地图片");
            }

            // 更新胃溃疡的图片为本地图片
            int updatedGastricUlcer = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/gastric-ulcer.webp",
                "胃溃疡"
            );
            if (updatedGastricUlcer > 0) {
                log.info("✅ 已更新胃溃疡的疾病图片为本地图片");
            }

            // 更新口腔溃疡的图片为本地图片
            int updatedMouthUlcer = jdbcTemplate.update(
                "UPDATE `illness` SET `img_path` = ? WHERE `illness_name` = ?",
                "/assets/images/illness/mouth-ulcer.webp",
                "口腔溃疡"
            );
            if (updatedMouthUlcer > 0) {
                log.info("✅ 已更新口腔溃疡的疾病图片为本地图片");
            }

            // ====== 药品图片 ======
            // 更新阿莫西林胶囊的图片为本地图片
            int updatedAmoxicillin = jdbcTemplate.update(
                "UPDATE `medicine` SET `img_path` = ? WHERE `medicine_name` = ?",
                "/assets/images/medicine/amoxicillin.jpg",
                "阿莫西林胶囊"
            );
            if (updatedAmoxicillin > 0) {
                log.info("✅ 已更新阿莫西林胶囊的药品图片为本地图片");
            }

            // 更新999感冒灵颗粒的图片为本地图片
            int updatedGanmaoling = jdbcTemplate.update(
                "UPDATE `medicine` SET `img_path` = ? WHERE `medicine_name` = ?",
                "/assets/images/medicine/ganmaoling.webp",
                "999感冒灵颗粒"
            );
            if (updatedGanmaoling > 0) {
                log.info("✅ 已更新999感冒灵颗粒的药品图片为本地图片");
            }

            // 更新开塞露的图片为本地图片
            int updatedKasailu = jdbcTemplate.update(
                "UPDATE `medicine` SET `img_path` = ? WHERE `medicine_name` = ?",
                "/assets/images/medicine/kasailu.jpg",
                "开塞露"
            );
            if (updatedKasailu > 0) {
                log.info("✅ 已更新开塞露的药品图片为本地图片");
            }

            // 更新三九胃泰颗粒的图片为本地图片
            int updatedWeita = jdbcTemplate.update(
                "UPDATE `medicine` SET `img_path` = ? WHERE `medicine_name` = ?",
                "/assets/images/medicine/weita颗粒.jpg",
                "三九胃泰颗粒"
            );
            if (updatedWeita > 0) {
                log.info("✅ 已更新三九胃泰颗粒的药品图片为本地图片");
            }

            // 更新999皮炎平的图片为本地图片
            int updatedPiyanping = jdbcTemplate.update(
                "UPDATE `medicine` SET `img_path` = ? WHERE `medicine_name` = ?",
                "/assets/images/medicine/piyanping.jpg",
                "999皮炎平"
            );
            if (updatedPiyanping > 0) {
                log.info("✅ 已更新999皮炎平的药品图片为本地图片");
            }

            // 更新甲硝唑的图片为本地图片
            int updatedJiaxiaozuo = jdbcTemplate.update(
                "UPDATE `medicine` SET `img_path` = ? WHERE `medicine_name` = ?",
                "/assets/images/medicine/jiaxiaozuo.jpg",
                "甲硝唑"
            );
            if (updatedJiaxiaozuo > 0) {
                log.info("✅ 已更新甲硝唑的药品图片为本地图片");
            }

            // 更新布洛芬缓释胶囊的图片为本地图片
            int updatedIbuprofen = jdbcTemplate.update(
                "UPDATE `medicine` SET `img_path` = ? WHERE `medicine_name` = ?",
                "/assets/images/medicine/ibuprofen.webp",
                "布洛芬缓释胶囊"
            );
            if (updatedIbuprofen > 0) {
                log.info("✅ 已更新布洛芬缓释胶囊的药品图片为本地图片");
            }

            // ====== 管理员头像 ======
            int updatedAdminAvatar = jdbcTemplate.update(
                "UPDATE `user` SET `img_path` = ? WHERE `user_account` = ?",
                "/assets/images/team/user-1.jpg",
                "admin"
            );
            if (updatedAdminAvatar > 0) {
                log.info("✅ 已更新管理员头像为本地图片");
            }

            // 更新默认用户头像
            int updatedUserAvatar = jdbcTemplate.update(
                "UPDATE `user` SET `img_path` = ? WHERE `user_account` = ? AND `img_path` LIKE 'http%'",
                "/assets/images/team/user-2.jpg",
                "zhangsan"
            );
            if (updatedUserAvatar > 0) {
                log.info("✅ 已更新张三头像为本地图片");
            }
        } catch (Exception e) {
            log.error("数据库迁移失败", e);
        }
    }
}