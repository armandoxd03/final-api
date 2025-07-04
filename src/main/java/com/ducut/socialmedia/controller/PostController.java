package com.ducut.socialmedia.controller;

import com.ducut.socialmedia.model.Comment;
import com.ducut.socialmedia.model.Post;
import com.ducut.socialmedia.repository.CommentRepository;
import com.ducut.socialmedia.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        try {
            List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            logger.error("Error fetching posts: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Post post) {
        try {
            if ((post.getContent() == null || post.getContent().trim().isEmpty()) &&
                    (post.getImageUrl() == null || post.getImageUrl().trim().isEmpty()) &&
                    (post.getVideoUrl() == null || post.getVideoUrl().trim().isEmpty())) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Validation failed",
                                "message", "Post must contain either content, image, or video")
                );
            }

            if (post.getUsername() == null || post.getUsername().trim().isEmpty()) {
                post.setUsername("Anonymous");
            }
            if (post.getUserImageUrl() == null || post.getUserImageUrl().trim().isEmpty()) {
                post.setUserImageUrl("https://randomuser.me/api/portraits/lego/1.jpg");
            }

            post.setContent(post.getContent() != null ? post.getContent().trim() : null);
            post.setImageUrl(post.getImageUrl() != null ? post.getImageUrl().trim() : null);
            post.setVideoUrl(post.getVideoUrl() != null ? post.getVideoUrl().trim() : null);

            post.setCreatedAt(LocalDateTime.now());
            post.setUpdatedAt(LocalDateTime.now());

            Post savedPost = postRepository.save(post);
            return ResponseEntity.ok(savedPost);
        } catch (Exception e) {
            logger.error("Error creating post: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", e.getMessage()
                    ));
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> createPostsBulk(@RequestBody List<Post> posts) {
        try {
            // Set defaults and timestamps for each post
            posts.forEach(post -> {
                if (post.getUsername() == null || post.getUsername().trim().isEmpty()) {
                    post.setUsername("Anonymous");
                }
                if (post.getUserImageUrl() == null || post.getUserImageUrl().trim().isEmpty()) {
                    post.setUserImageUrl("https://randomuser.me/api/portraits/lego/1.jpg");
                }

                post.setContent(post.getContent() != null ? post.getContent().trim() : null);
                post.setImageUrl(post.getImageUrl() != null ? post.getImageUrl().trim() : null);
                post.setVideoUrl(post.getVideoUrl() != null ? post.getVideoUrl().trim() : null);

                // Set timestamps to now
                LocalDateTime now = LocalDateTime.now();
                post.setCreatedAt(now);
                post.setUpdatedAt(now);

                // Ensure ID is null so the database can generate it (if using @GeneratedValue)
                post.setId(null);
            });

            // Save all posts; IDs will be generated by DB if configured
            List<Post> savedPosts = postRepository.saveAll(posts);

            // Return saved posts with generated IDs
            return ResponseEntity.ok(savedPosts);
        } catch (Exception e) {
            logger.error("Error bulk creating posts: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", e.getMessage()
                    ));
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        try {
            Optional<Post> post = postRepository.findById(id);
            return post.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching post by ID: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody Post postDetails) {
        try {
            return postRepository.findById(id)
                    .map(post -> {
                        post.setContent(postDetails.getContent() != null ?
                                postDetails.getContent().trim() : null);
                        post.setUsername(postDetails.getUsername() != null ?
                                postDetails.getUsername().trim() : null);
                        post.setUserImageUrl(postDetails.getUserImageUrl() != null ?
                                postDetails.getUserImageUrl().trim() : null);
                        post.setImageUrl(postDetails.getImageUrl() != null ?
                                postDetails.getImageUrl().trim() : null);
                        post.setVideoUrl(postDetails.getVideoUrl() != null ?
                                postDetails.getVideoUrl().trim() : null);
                        post.setUpdatedAt(LocalDateTime.now());
                        return ResponseEntity.ok(postRepository.save(post));
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error updating post: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", e.getMessage()
                    ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        try {
            if (postRepository.existsById(id)) {
                postRepository.deleteById(id);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting post: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likePost(@PathVariable Long id) {
        try {
            return postRepository.findById(id)
                    .map(post -> {
                        post.setLikeCount(post.getLikeCount() + 1);
                        post.setUpdatedAt(LocalDateTime.now());
                        return ResponseEntity.ok(postRepository.save(post));
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error liking post: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", e.getMessage()
                    ));
        }
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> sharePost(@PathVariable Long id) {
        try {
            return postRepository.findById(id)
                    .map(post -> {
                        post.setShareCount(post.getShareCount() + 1);
                        post.setUpdatedAt(LocalDateTime.now());
                        return ResponseEntity.ok(postRepository.save(post));
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error sharing post: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Comment>> getCommentsByPostId(@PathVariable Long postId) {
        try {
            List<Comment> comments = commentRepository.findByPostId(postId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            logger.error("Error fetching comments: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long postId,
            @RequestBody Comment commentRequest) {
        try {
            return postRepository.findById(postId)
                    .map(post -> {
                        Comment comment = new Comment(
                                commentRequest.getUsername() != null ?
                                        commentRequest.getUsername().trim() : "Anonymous",
                                commentRequest.getUserImageUrl() != null ?
                                        commentRequest.getUserImageUrl().trim() :
                                        "https://randomuser.me/api/portraits/lego/1.jpg",
                                commentRequest.getContent() != null ?
                                        commentRequest.getContent().trim() : null,
                                commentRequest.getImageUrl() != null ?
                                        commentRequest.getImageUrl().trim() : null,
                                commentRequest.getVideoUrl() != null ?
                                        commentRequest.getVideoUrl().trim() : null,
                                post
                        );
                        commentRepository.save(comment);
                        return ResponseEntity.ok(postRepository.findById(postId).get());
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error adding comment: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", e.getMessage()
                    ));
        }
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Comment> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody Comment commentRequest) {
        try {
            if (!postRepository.existsById(postId)) {
                return ResponseEntity.notFound().build();
            }

            return commentRepository.findById(commentId)
                    .map(comment -> {
                        comment.setContent(commentRequest.getContent() != null ?
                                commentRequest.getContent().trim() : null);
                        comment.setImageUrl(commentRequest.getImageUrl() != null ?
                                commentRequest.getImageUrl().trim() : null);
                        comment.setVideoUrl(commentRequest.getVideoUrl() != null ?
                                commentRequest.getVideoUrl().trim() : null);
                        return ResponseEntity.ok(commentRepository.save(comment));
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error updating comment: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        try {
            if (!postRepository.existsById(postId)) {
                return ResponseEntity.notFound().build();
            }

            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isPresent()) {
                Comment comment = commentOpt.get();
                if (!comment.getPost().getId().equals(postId)) {
                    return ResponseEntity.notFound().build();
                }
                commentRepository.delete(comment);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting comment: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }



    @PostMapping("/{postId}/comments/{commentId}/like")
    public ResponseEntity<Comment> likeComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        try {
            if (!postRepository.existsById(postId)) {
                return ResponseEntity.notFound().build();
            }

            return commentRepository.findById(commentId)
                    .map(comment -> {
                        comment.setLikeCount(comment.getLikeCount() + 1);
                        return ResponseEntity.ok(commentRepository.save(comment));
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error liking comment: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}