/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class MoreLuckyController {

	private static final AtomicLong currentId = new AtomicLong(0L);
	private Map<Long,Video> videos = new HashMap<Long, Video>();

	@GetMapping("/video")
	@ResponseBody
	public Collection<Video> getVideos() {
		return videos.values();
	}

	@PostMapping("/video")
	@ResponseBody
	public Video addVideo(@RequestBody Video entity) {
		//set id
		if(entity.getId() == 0)
			entity.setId(currentId.incrementAndGet());
		//set url
		String url = getUrlBaseForLocalServer() + "/video/" + entity.getId() + "/data";
		entity.setDataUrl(url);
		//save
		videos.put(entity.getId(), entity);
		return entity;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base = "http://"+request.getServerName() + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
		return base;
	}

	@GetMapping("/video/{id}/data")
	@ResponseBody
	public ResponseEntity<byte[]> getVideoData(@PathVariable("id") long id) {
		Video video = videos.get(id);
		if (video==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "video id not found");
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			VideoFileManager.get().copyVideoData(video, out);
			return ResponseEntity.ok(out.toByteArray());
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.NO_CONTENT, "cannot read video");
		}
	}


	@PostMapping("/video/{id}/data")
	@ResponseBody
	public VideoStatus addVideoData(
			@PathVariable("id") long id,
			@RequestParam("data") MultipartFile data) {
		Video video = videos.get(id);
		if (video==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "video id not found");
		try {
			VideoFileManager.get().saveVideoData(video, data.getInputStream());
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.NO_CONTENT, "cannot read video");
		}
		return new VideoStatus(VideoState.READY);
	}



}
