package world.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import world.dto.RespResult;
import world.entity.Illness;

import java.util.List;


/**
 * 疾病控制器
 *
 */
@RestController
@RequestMapping("illness")
public class IllnessController extends BaseController<Illness> {

    /**
     * 按名称模糊搜索疾病（用于药品关联时的疾病搜索）
     *
     * @param keyword 搜索关键词
     * @return 匹配的疾病列表（最多 20 条）
     */
    @GetMapping("/search")
    public RespResult search(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return RespResult.fail("搜索关键词不能为空");
        }
        List<Illness> list = illnessService.searchByName(keyword.trim());
        return RespResult.success("查询成功", list);
    }
}
