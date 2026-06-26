package world.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import world.entity.MedicalNews;


/**
 * 咨询控制器
 *
 */
@RestController
@RequestMapping("medical_news")
public class MedicalNewsController extends BaseController<MedicalNews> {


}
