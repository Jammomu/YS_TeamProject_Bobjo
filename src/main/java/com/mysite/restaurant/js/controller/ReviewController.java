package com.mysite.restaurant.js.controller;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.mysite.restaurant.hj.jwt.JwtTokenProvider;
import com.mysite.restaurant.js.model.*;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import com.mysite.restaurant.js.service.ReviewService;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private JwtTokenProvider tokenProvider;

    private final MinioClient minioClient;
    private final ReviewService reviewService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Autowired
    public ReviewController(MinioClient minioClient, ReviewService reviewService, JwtTokenProvider tokenProvider) {
        this.minioClient = minioClient;
        this.reviewService = reviewService;
        this.tokenProvider = tokenProvider; // 추가
    }

    // 가게 리뷰와 리뷰 이미지 및 좋아요 상태 조회
    @GetMapping("/restaurants/{restaurant_id}/reviews")
    public Map<String, Object> getReviewsWithImages(
            @PathVariable("restaurant_id") Long restaurantId,
            @RequestHeader("Authorization") String token // Authorization 헤더 추가
    ) {
        // "Bearer " 제거
        String jwtToken = token.replace("Bearer ", "");

        // 토큰에서 사용자 ID 추출
        Long userId = tokenProvider.getUserId(jwtToken);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token");
        }

        // 가게 리뷰 조회
        List<Reviews> reviews = reviewService.selectRestaurantReviews(restaurantId);

        // 리뷰의 이미지
        List<Map<String, Object>> reviewList = reviews.stream().map(review -> {
            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("review", review);
            return reviewData;
        }).collect(Collectors.toList());

        Map<Long, List<Map<String, Object>>> imagesMap = reviews.stream().collect(Collectors.toMap(
                Reviews::getReviewId,
                review -> reviewService.selectReviewImg(review.getReviewId())
                        .stream()
                        .map(image -> {
                            Map<String, Object> imageData = new HashMap<>();
                            imageData.put("imageUrl", image.getImageUrl());
                            return imageData;
                        })
                        .collect(Collectors.toList())
        ));

        Map<Long, Boolean> isHelpfulMap = reviews.stream().collect(Collectors.toMap(
                Reviews::getReviewId,
                review -> reviewService.isHelpfulExist(review.getReviewId(), userId)
        ));

        Map<Long, Integer> helpfulCountMap = reviews.stream().collect(Collectors.toMap(
                Reviews::getReviewId,
                review -> reviewService.getHelpfulCount(review.getReviewId())
        ));

        // 결과를 Map에 묶어서 반환
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviewList);
        result.put("images", imagesMap);
        result.put("isHelpful", isHelpfulMap);
        result.put("helpfulCounts", helpfulCountMap);

        return result;
    }

    // 내 리뷰와 리뷰 이미지 조회
    @GetMapping("/reviews/mypage")
    public Map<String, Object> getMyReviewsWithImages(@RequestHeader("Authorization") String token) {
        // "Bearer " 제거
        String jwtToken = token.replace("Bearer ", "");

        // 토큰에서 사용자 ID 추출
        Long userId = tokenProvider.getUserId(jwtToken);

        // 사용자 리뷰 조회
        List<Reviews> reviews = reviewService.selectMyReviews(userId);

        // 리뷰 이미지 조회
        List<ReviewImg> reviewImages = reviewService.selectReviewImg(userId);

        // 레스토랑 조회
        List<Restaurants> restaurants = reviewService.selectMyRestaurants(userId);

        // 결과를 Map으로 묶어서 반환
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviews);
        result.put("reviewImages", reviewImages);
        result.put("restaurants", restaurants);

        return result;
    }

    //리뷰 작성
    @PostMapping("/reviews")
    public ResponseEntity<?> createReview(
            @RequestParam("review_content") String reviewContent,
            @RequestParam("restaurant_id") Long restaurantId,
            @RequestParam("user_id") Long userId,
            @RequestParam("rating") Integer rating,
            @RequestParam("reservation_id") Long reservationId,
            @RequestParam(value = "images", required = false) MultipartFile[] images) throws IOException {

        // 예약 정보 가져오기
        Reservation reservation = reviewService.selectReservation(reservationId);

        if (reservation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Reservation not found.");
        }

        // 예약 시간과 현재 시간 비교
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reviewPossibleTime = reservation.getReservationTime().plusMinutes(30);

        if (now.isBefore(reviewPossibleTime)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "예약 시간 30분 후 부터 작성하실 수 있습니다.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        // 리뷰 저장
        Reviews review = new Reviews();
        review.setReviewContent(reviewContent);
        review.setRestaurantId(restaurantId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setReservationId(reservationId);

        Long reviewId = reviewService.insertReview(review);

        // 이미지 업로드 처리
        if (images != null && images.length > 0) {
            try {
                for (int i = 0; i < images.length; i++) {
                    MultipartFile image = images[i];

                    // 파일 확장자 추출
                    String originalFilename = image.getOriginalFilename();
                    String extension = "";
                    if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    }

                    // 새로운 파일명 생성
                    String newFileName = UUID.randomUUID().toString() + extension;

                    // 임시 파일 생성
                    File tempFile = File.createTempFile("upload-", extension);
                    image.transferTo(tempFile);

                    // MinIO에 파일 업로드
                    try {
                        minioClient.uploadObject(
                                UploadObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object("reviews/" + newFileName)
                                        .filename(tempFile.getAbsolutePath()) // 임시 파일 경로
                                        .build()
                        );
                    } catch (Exception e) {
                        logger.error("Failed to upload to MinIO: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("MinIO upload failed: " + e.getMessage());
                    } finally {
                        tempFile.delete(); // 임시 파일 삭제
                    }

                    // 업로드된 파일 URL 생성
                    String imageUrl = "/reviews/" + newFileName;

                    // ReviewImg DTO 설정 및 저장
                    ReviewImg reviewImg = new ReviewImg();
                    reviewImg.setReviewId(reviewId);
                    reviewImg.setImageUrl(imageUrl);
                    reviewImg.setImageOrder(i + 1);
                    reviewService.insertReviewImage(reviewImg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Image upload failed: " + e.getMessage());
            }
        }

        // 성공적으로 리뷰 생성
        return ResponseEntity.status(HttpStatus.CREATED).body("Review created successfully with ID: " + reviewId);
    }

    // 답글 조회
    @GetMapping("/reviews/{review_id}/replies")
    public List<Replies> getReplies(@PathVariable("review_id") Long reviewId) {
        return reviewService.selectReplies(reviewId);
    }

    // 답글 작성
    @PostMapping("/reviews/{review_id}/replies")
    public int createReplies(@PathVariable("review_id") Long reviewId, @RequestBody Replies replies) {
        replies.setReviewId(reviewId);
        return reviewService.insertReplie(replies);
    }

    // 리뷰 수정
    @PutMapping("/reviews/{review_id}")
    public int updateReview(@PathVariable("review_id") Long reviewId, @RequestBody Reviews review) {
        review.setReviewId(reviewId);
        return reviewService.updateReview(review);
    }

    // 리뷰 삭제
    @DeleteMapping("/reviews/{review_id}")
    public void deleteReview(@PathVariable("review_id") Long reviewId) {
        reviewService.deleteReview(reviewId);
    }

    // 신고 조회
    @GetMapping("/js/reports/mypage/{user_id}")
    public ResponseEntity<List<Map<String, Object>>> getMyReports(@PathVariable("user_id") Long userId) {
        List<Map<String, Object>> reports = reviewService.getMyReports(userId);
        return ResponseEntity.ok(reports);
    }
    // 가게 신고리뷰 조회
    @GetMapping("/js/reports/{restaurant_id}")
    public ResponseEntity<List<Map<String, Object>>> getReportsDetails(@PathVariable("restaurant_id") Long restaurantId) {
        List<Map<String, Object>> reports = reviewService.getReports(restaurantId);
        return ResponseEntity.ok(reports);
    }

    // 리뷰 신고 작성
    @PostMapping("/reviews/{review_id}/report")
    public int createReport(@PathVariable("review_id") Long reviewId, @RequestBody Reports reports) {
        reports.setReviewId(reviewId);
        return reviewService.insertReport(reports);
    }

    // 신고 삭제
    @DeleteMapping("/js/reports/{report_id}")
    public void deleteReport(@PathVariable("report_id") Long reportId) {
        reviewService.deleteReport(reportId);
    }


    // 도움 등록/삭제 API
    @PostMapping("/reviews/{review_id}/helpful")
    public ResponseEntity<String> addHelpful(@PathVariable("review_id") Long reviewId, @RequestBody Helpful helpful) {
        try {
            helpful.setReviewId(reviewId);
            reviewService.addHelpful(helpful);
            return ResponseEntity.status(HttpStatus.OK).body("도움이 되었습니다.");
        } catch (Exception e) {
            logger.error("도움 등록 중 오류가 발생했습니다. 리뷰 ID: " + reviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("도움 등록 중 오류가 발생했습니다.");
        }
    }

    @DeleteMapping("/reviews/{review_id}/helpful")
    public ResponseEntity<String> removeHelpful(@PathVariable("review_id") Long reviewId, @RequestParam Long userId) {
        try {
            reviewService.removeHelpful(reviewId, userId);
            return ResponseEntity.status(HttpStatus.OK).body("도움이 취소되었습니다.");
        } catch (Exception e) {
            logger.error("도움 취소 중 오류가 발생했습니다. 리뷰 ID: " + reviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("도움 취소 중 오류가 발생했습니다.");
        }
    }


    // 가게 정보
    @GetMapping("/restaurants/{restaurant_id}")
    public ResponseEntity<Map<String, Object>> getShopDetails(@PathVariable("restaurant_id") Long restaurantId) {
    	
        // 가게 정보*이미지 조회
        Restaurants restaurant = reviewService.selectShop(restaurantId);
        List<RestaurantImg> restaurantImg = reviewService.selectShopImg(restaurantId);
        // 결과를 Map에 담기
        Map<String, Object> response = new HashMap<>();
        response.put("restaurant", restaurant);
        response.put("restaurantImg", restaurantImg);

        return ResponseEntity.ok(response);
    }

    // 예약 조회
    @GetMapping("/js/reservation/{reservation_id}")
    public Reservation getReservation(@PathVariable("reservation_id") Long reservationId) {
        return reviewService.selectReservation(reservationId);
    }
    // 유저 조회
    @GetMapping("/js/user/{user_id}")
    public User getUser(@PathVariable("user_id") Long userId) {
        return reviewService.selectUser(userId);
    }
    // 모든 유저 조회
    @GetMapping("/js/users/{restaurant_id}")
    public List<User> getAllUsers(@PathVariable("restaurant_id") Long restaurantId) {
        return reviewService.getAllUsers(restaurantId);
    }
}
