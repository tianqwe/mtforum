package com.forum.mt.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;

import android.view.animation.DecelerateInterpolator;

/**
 * 动画工具类 - 封装常用动画效果
 */
public class AnimationHelper {

    // 动画时长常量
    public static final int DURATION_FAST = 150;
    public static final int DURATION_NORMAL = 250;
    public static final int DURATION_SLOW = 350;

    /**
     * 平移动画 - 从右侧滑入
     */
    public static void slideInFromRight(View view) {
        slideInFromRight(view, DURATION_NORMAL);
    }

    public static void slideInFromRight(View view, long duration) {
        slideInFromRight(view, duration, null);
    }

    public static void slideInFromRight(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.setTranslationX(view.getResources().getDisplayMetrics().widthPixels);
        view.animate()
                .translationX(0)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 平移动画 - 从左侧滑入
     */
    public static void slideInFromLeft(View view) {
        slideInFromLeft(view, DURATION_NORMAL);
    }

    public static void slideInFromLeft(View view, long duration) {
        slideInFromLeft(view, duration, null);
    }

    public static void slideInFromLeft(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.setTranslationX(-view.getResources().getDisplayMetrics().widthPixels);
        view.animate()
                .translationX(0)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 平移动画 - 从底部滑入
     */
    public static void slideInFromBottom(View view) {
        slideInFromBottom(view, DURATION_NORMAL);
    }

    public static void slideInFromBottom(View view, long duration) {
        slideInFromBottom(view, duration, null);
    }

    public static void slideInFromBottom(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.setTranslationY(view.getResources().getDisplayMetrics().heightPixels);
        view.animate()
                .translationY(0)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 平移动画 - 从顶部滑入
     */
    public static void slideInFromTop(View view) {
        slideInFromTop(view, DURATION_NORMAL);
    }

    public static void slideInFromTop(View view, long duration) {
        slideInFromTop(view, duration, null);
    }

    public static void slideInFromTop(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.setTranslationY(-view.getResources().getDisplayMetrics().heightPixels);
        view.animate()
                .translationY(0)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 平移动画 - 向右滑出
     */
    public static void slideOutToRight(View view) {
        slideOutToRight(view, DURATION_NORMAL);
    }

    public static void slideOutToRight(View view, long duration) {
        slideOutToRight(view, duration, null);
    }

    public static void slideOutToRight(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.animate()
                .translationX(view.getResources().getDisplayMetrics().widthPixels)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 平移动画 - 向左滑出
     */
    public static void slideOutToLeft(View view) {
        slideOutToLeft(view, DURATION_NORMAL);
    }

    public static void slideOutToLeft(View view, long duration) {
        slideOutToLeft(view, duration, null);
    }

    public static void slideOutToLeft(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.animate()
                .translationX(-view.getResources().getDisplayMetrics().widthPixels)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 平移动画 - 向下滑出
     */
    public static void slideOutToBottom(View view) {
        slideOutToBottom(view, DURATION_NORMAL);
    }

    public static void slideOutToBottom(View view, long duration) {
        slideOutToBottom(view, duration, null);
    }

    public static void slideOutToBottom(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.animate()
                .translationY(view.getResources().getDisplayMetrics().heightPixels)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 平移动画 - 向上滑出
     */
    public static void slideOutToTop(View view) {
        slideOutToTop(view, DURATION_NORMAL);
    }

    public static void slideOutToTop(View view, long duration) {
        slideOutToTop(view, duration, null);
    }

    public static void slideOutToTop(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.animate()
                .translationY(-view.getResources().getDisplayMetrics().heightPixels)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 淡入动画
     */
    public static void fadeIn(View view) {
        fadeIn(view, DURATION_NORMAL);
    }

    public static void fadeIn(View view, long duration) {
        fadeIn(view, duration, null);
    }

    public static void fadeIn(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 淡出动画
     */
    public static void fadeOut(View view) {
        fadeOut(view, DURATION_NORMAL);
    }

    public static void fadeOut(View view, long duration) {
        fadeOut(view, duration, null);
    }

    public static void fadeOut(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.animate()
                .alpha(0f)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 缩放淡入动画
     */
    public static void scaleIn(View view) {
        scaleIn(view, DURATION_NORMAL);
    }

    public static void scaleIn(View view, long duration) {
        scaleIn(view, duration, null);
    }

    public static void scaleIn(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f),
                ObjectAnimator.ofFloat(view, View.ALPHA, 1f)
        );
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (endAction != null) {
                    endAction.run();
                }
            }
        });
        animatorSet.start();
    }

    /**
     * 缩放淡出动画
     */
    public static void scaleOut(View view) {
        scaleOut(view, DURATION_NORMAL);
    }

    public static void scaleOut(View view, long duration) {
        scaleOut(view, duration, null);
    }

    public static void scaleOut(View view, long duration, Runnable endAction) {
        if (view == null) return;

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 0f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f),
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f)
        );
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                if (endAction != null) {
                    endAction.run();
                }
            }
        });
        animatorSet.start();
    }

    /**
     * 自定义距离的平移动画
     */
    public static void translateX(View view, float fromX, float toX) {
        translateX(view, fromX, toX, DURATION_NORMAL);
    }

    public static void translateX(View view, float fromX, float toX, long duration) {
        translateX(view, fromX, toX, duration, null);
    }

    public static void translateX(View view, float fromX, float toX, long duration, Runnable endAction) {
        if (view == null) return;

        view.setTranslationX(fromX);
        view.animate()
                .translationX(toX)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 自定义距离的Y轴平移动画
     */
    public static void translateY(View view, float fromY, float toY) {
        translateY(view, fromY, toY, DURATION_NORMAL);
    }

    public static void translateY(View view, float fromY, float toY, long duration) {
        translateY(view, fromY, toY, duration, null);
    }

    public static void translateY(View view, float fromY, float toY, long duration, Runnable endAction) {
        if (view == null) return;

        view.setTranslationY(fromY);
        view.animate()
                .translationY(toY)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                })
                .start();
    }

    /**
     * 组合动画：先淡入再从底部滑入
     */
    public static void fadeInAndSlideInFromBottom(View view) {
        fadeInAndSlideInFromBottom(view, DURATION_NORMAL);
    }

    public static void fadeInAndSlideInFromBottom(View view, long duration) {
        fadeInAndSlideInFromBottom(view, duration, null);
    }

    public static void fadeInAndSlideInFromBottom(View view, long duration, Runnable endAction) {
        if (view == null) return;

        view.setTranslationY(view.getResources().getDisplayMetrics().heightPixels / 4);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0),
                ObjectAnimator.ofFloat(view, View.ALPHA, 1f)
        );
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (endAction != null) {
                    endAction.run();
                }
            }
        });
        animatorSet.start();
    }

    /**
     * 取消所有正在进行的动画
     */
    public static void cancelAnimation(View view) {
        if (view != null) {
            view.animate().cancel();
        }
    }

    /**
     * 重置视图状态（取消动画并重置属性）
     */
    public static void resetViewState(View view) {
        if (view == null) return;

        view.animate().cancel();
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setRotation(0f);
        view.setRotationX(0f);
        view.setRotationY(0f);
    }
}
