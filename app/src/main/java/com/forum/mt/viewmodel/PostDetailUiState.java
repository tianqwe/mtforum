package com.forum.mt.viewmodel;

import com.forum.mt.model.Comment;
import com.forum.mt.model.Post;

import java.util.List;

/**
 * 帖子详情 UI 状态
 */
public abstract class PostDetailUiState {
    
    public static class Loading extends PostDetailUiState {}
    
    public static class Success extends PostDetailUiState {
        private final Post post;
        private final List<Comment> comments;
        
        public Success(Post post) {
            this(post, null);
        }
        
        public Success(Post post, List<Comment> comments) {
            this.post = post;
            this.comments = comments;
        }
        
        public Post getPost() {
            return post;
        }
        
        public List<Comment> getComments() {
            return comments;
        }
    }
    
    public static class Error extends PostDetailUiState {
        private final String message;
        
        public Error(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
