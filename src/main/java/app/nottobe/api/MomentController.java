package app.nottobe.api;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import app.nottobe.bean.Image;
import app.nottobe.bean.Moment;
import app.nottobe.bean.User;
import app.nottobe.component.OssUploader;
import app.nottobe.entity.Result;
import app.nottobe.repository.MomentRepository;

@RestController
@RequestMapping("moment")
public class MomentController extends BaseController {

	@Autowired
	private OssUploader ossUploader;

	@Autowired
	private MomentRepository momentRepository;

	@GetMapping("list")
	public Result<Iterable<Moment>> list(String code) {
		Iterable<Moment> iterable = momentRepository.findAll();
		return Result.getResult(iterable);
	}

	@PostMapping("post_text")
	public Result<Moment> post_text(HttpServletRequest request, @RequestParam(required = true) String text) {
		User user = authorized(request);
		Moment moment = new Moment();
		moment.setAuthor(user);
		moment.setText(text);
		moment = momentRepository.save(moment);
		return Result.getResult(moment);
	}

	@PostMapping("post_images")
	public Result<Moment> post_images(HttpServletRequest request,
			@RequestParam(required = false, defaultValue = "") String text) {
		User user = authorized(request);
		MultipartRequest fileRequest = (MultipartRequest) request;
		List<MultipartFile> files = fileRequest.getFiles("images");
		if (files == null || files.size() == 0) {
			return Result.getErrorResult("请上传图片");
		}
		Set<Image> images = new HashSet<Image>();
		for (MultipartFile multipartFile : files) {
			if (!multipartFile.isEmpty() && multipartFile.getContentType().startsWith("image/")) {
				String originFilename = multipartFile.getOriginalFilename();
				String filename = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + originFilename;
				String filepath = "ntb/" + filename;
				try {
					String url = ossUploader.uploadFile(filepath, multipartFile.getInputStream());
					Image image = new Image();
					image.setAuthor(user);
					image.setUrl(url);
					images.add(image);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		Moment moment = new Moment();
		moment.setAuthor(user);
		moment.setText(text);
		moment.setImages(images);
		moment = momentRepository.save(moment);
		return Result.getResult(moment);
	}

	@PostMapping("delete")
	public Result<Boolean> delete(HttpServletRequest request, @RequestParam(required = true) long id) {
		User user = authorized(request);
		Moment moment = momentRepository.findOne(id);
		if (moment == null) {
			return Result.getErrorResult("删除失败");
		}
		if (moment.getAuthor() == null) {
			return Result.getErrorResult("删除失败!");
		}
		if (user.getId() != moment.getAuthor().getId()) {
			return Result.getUnauthorizedErrorResult("没有权限");
		}
		momentRepository.delete(id);
		return Result.getResult(true);
	}
}
