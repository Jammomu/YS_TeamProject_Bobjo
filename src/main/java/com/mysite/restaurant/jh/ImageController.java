package com.mysite.restaurant.jh;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ImageController {
    private final RestaurantService restaurantService;
    private final MinioClient minioClient;
    
    // application.properties에서 설정된 upload-dir 값을 주입받음
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${minio.url}")
    private String minioUrl;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable("filename") String filename) {
        try {
            // 이미지 확장자 기반으로 MIME 타입 설정
            String fileExtension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            String mimeType = "image/jpeg";  // 기본값
            if (fileExtension.equals("png")) {
                mimeType = "image/png";
            } else if (fileExtension.equals("gif")) {
                mimeType = "image/gif";
            }

            // 로컬 파일 경로 확인
            String filePath = Paths.get(uploadDir, filename).toString();
            System.out.println("File path: " + filePath); // 경로 로그 출력

            Resource file = new FileSystemResource(filePath);

            // 파일이 존재하는지 확인
            if (file.exists()) {
                System.out.println("File found: " + filename);  // 파일이 존재하면 출력
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, mimeType)
                        .body(file);
            } else {
                System.out.println("File not found: " + filename);  // 파일이 없으면 출력
                return ResponseEntity.notFound().build(); // 파일이 없으면 404 오류 반환
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build(); // 에러 발생 시 500 오류 반환
        }
    }

    
    // 이미지 조회하기
    @GetMapping("/api/restaurant/{restaurantId}/image")
    public ResponseEntity<List<ImageDTO>> getRestaurantImageById(@PathVariable("restaurantId") int restaurantId) {
        // 이미지 리스트 가져오기
        List<ImageDTO> images = restaurantService.getRestaurantImageById(restaurantId);
        
//        // 이미지 URL을 절대 경로로 변환
//        String baseUrl = "https://storage.cofile.co.kr/ysit24restaurant-bucket/images"; // 실제 서버 주소로 변경해야 함
//
//        // 이미지 URL을 절대 경로로 수정
//        for (ImageDTO image : images) {
//            // "/images/"를 제외하고 절대 경로로 추가
//            String originalUrl = image.getImageUrl(); // 변환 전 URL 확인
//            String updatedUrl = baseUrl + originalUrl.substring(7); // 절대 경로로 변환
//
//            // 변환 전후 URL 콘솔에 출력
//            System.out.println("Original URL: " + originalUrl);
//            System.out.println("Updated URL: " + updatedUrl);
//            System.out.println("uploadDir URL: " + uploadDir);
//            image.setImageUrl(baseUrl + image.getImageUrl().substring(7));
//        }

        return ResponseEntity.ok(images);  // 200 OK와 함께 수정된 이미지 목록 반환
    }

 // 이미지 등록하기
    @PostMapping("/api/restaurant/{restaurantId}/image/insert")
    public ResponseEntity<Void> insertImage(
            @PathVariable("restaurantId") int restaurantId,  // URL 경로에서 restaurantId 받기
            @RequestParam("file") MultipartFile file,  // 파일을 받아오는 부분
            @RequestParam("imageOrder") Boolean imageOrder,  // 이미지 순서를 받기
            @RequestParam("uid") String uid) {  // UID를 받아오는 부분

        // ImageDTO 생성 및 설정
        ImageDTO image = new ImageDTO();
        image.setRestaurantId(restaurantId);
        image.setUid(uid);
        image.setImageOrder(imageOrder);  // 이미지 순서 설정

        // 파일 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 새로운 파일명 생성 (UUID 사용)
        String newFileName = UUID.randomUUID().toString() + extension;

        // 업로드 디렉토리 절대 경로로 설정
        String absoluteUploadDir = Paths.get(uploadDir).toAbsolutePath().toString();

        // 디렉토리 확인 및 생성
        File dir = new File(absoluteUploadDir);
        if (!dir.exists()) {
            dir.mkdirs();  // 디렉토리가 없다면 생성
        }

        // 파일 저장
        File destFile = new File(absoluteUploadDir + File.separator + newFileName);
        try {
            file.transferTo(destFile);  // 파일 저장
            
         // MinIO 업로드
            try {
                minioClient.uploadObject(
                    UploadObjectArgs.builder()
                        .bucket(bucketName)
                        .object("images/" + newFileName)
                        .filename(destFile.getAbsolutePath()) // 파일 경로
                        .build());
            } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();  // 예외 로그 출력
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();  // 500 오류 반환
            }
            
            // 저장된 이미지의 URL을 설정 
            // URL에서 중복된 슬래시 제거
            String imageUrl = "/images/" + newFileName;
            image.setImageUrl(imageUrl);  
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // 이미지 정보 저장
        restaurantService.insertImage(image);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    // 이미지 삭제
    @DeleteMapping("/api/restaurant/{restaurantId}/image/{imageId}/delete")
    public ResponseEntity<Void> deleteImage(@PathVariable("restaurantId") int restaurantId, @PathVariable("imageId") int imageId) {
        restaurantService.deleteImage(restaurantId, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/restaurant/setMain")
    public ResponseEntity<String> setMainImage(@RequestParam("restaurantId") int restaurantId, @RequestBody ImageDTO imageDTO) {
        try {
            restaurantService.updateImage(imageDTO);  // 서비스 메서드 호출하여 이미지 업데이트
            return ResponseEntity.ok("대표 이미지가 설정되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("이미지 설정에 실패했습니다.");
        }
    }
}
