package android.support.v7.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

public class CustomGridLayoutManager extends RecyclerView.LayoutManager {
    private static final String TAG = "CustomGridLayoutManager";

    public static final int MAX_SPAN_CHANGE_PROCESS = 50;
    private SparseArray<Rect> itemRects = new SparseArray<>();
    private int mSpanCount;
    private int mToSpanCount;
    public int totalHeight = 0;
    private int verticalScrollOffset;
    int[] mCachedBorders;
    int[] mToCachedBorders;
    int mSpanChangeProcess;
    float mOffsetDelta;
    private int initOffset;

    public boolean isSpanChangeMode() {
        return isSpanChangeMode;
    }

    private boolean isSpanChangeMode;
    private ObjectAnimator spanChangeAnim;
    private Animator.AnimatorListener spanChangeListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            isSpanChangeMode = false;
            spanChangeAnim = null;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            isSpanChangeMode = false;
            spanChangeAnim = null;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    public void setSpanChangeProcess(int spanChangeProcess) {
        if (spanChangeProcess > MAX_SPAN_CHANGE_PROCESS)
            spanChangeProcess = MAX_SPAN_CHANGE_PROCESS;
        else if (spanChangeProcess < -MAX_SPAN_CHANGE_PROCESS)
            spanChangeProcess = -MAX_SPAN_CHANGE_PROCESS;
        this.mSpanChangeProcess = spanChangeProcess;
        Log.d(TAG, "spanChangeProcess:" + mSpanChangeProcess);
        updateSpanChangeProcess();
    }

    public int getSpanChangeProcess() {
        return mSpanChangeProcess;
    }

    public  void incChangeProcess() {
        if (!isSpanChangeMode) {
            Log.i(TAG, "must call in spanCountChange mode");
            return;
        }
        this.mSpanChangeProcess++;
        Log.d(TAG, "spanChangeProcess:" + mSpanChangeProcess);
        updateSpanChangeProcess();
    }

    public  void reduceChangeProcess() {
        if (!isSpanChangeMode) {
            Log.i(TAG, "must call in spanCountChange mode");
            return;
        }
        this.mSpanChangeProcess--;
        updateSpanChangeProcess();
    }

    private  void updateSpanChangeProcess() {
        if (!isSpanChangeMode) {
            Log.i(TAG, "must call in spanCountChange mode");
            return;
        }
        Log.d(TAG, "mSpanChangeProcess:"+this.mSpanChangeProcess);
        if (this.mSpanChangeProcess == MAX_SPAN_CHANGE_PROCESS) {
            mSpanCount += 2;
            mCachedBorders = null;
            mToCachedBorders = null;
            verticalScrollOffset = (int) (initOffset+ mOffsetDelta*Math.abs(mSpanChangeProcess));
            mSpanChangeProcess = 0;
            isSpanChangeMode = false;
            Log.d(TAG, "verticalScrollOffset:" + verticalScrollOffset);
            calculateChildrenSite();
        } else if (this.mSpanChangeProcess == -MAX_SPAN_CHANGE_PROCESS) {
            mSpanCount -= 2;
            mCachedBorders = null;
            mToCachedBorders = null;
            verticalScrollOffset = (int) (initOffset+ mOffsetDelta*Math.abs(mSpanChangeProcess));
            mSpanChangeProcess = 0;

            isSpanChangeMode = false;
            calculateChildrenSite();
        } else {
            if ((mSpanCount <= 2 && this.mSpanChangeProcess < 0) || (mSpanCount > 8 && this.mSpanChangeProcess > 0)) {
                mSpanChangeProcess = 0;
                isSpanChangeMode = false;
                return;
            }
            if (mSpanChangeProcess == 0)
                mToCachedBorders = null;
            if (mToCachedBorders == null) {
                initOffset=verticalScrollOffset;
                if (mSpanChangeProcess > 0)
                    mToSpanCount = mSpanCount + 2;
                else if (mSpanChangeProcess < 0)
                    mToSpanCount = mSpanCount - 2;

                int totalSpace;
                totalSpace = this.getWidth() - this.getPaddingRight() - this.getPaddingLeft();
                mToCachedBorders = calculateItemBorders(mToCachedBorders, mToSpanCount, totalSpace);
                //当发生偏移的时候，因为itemSize发生变化，为了显示当前画面，需要更新滑动距离
                if (mSpanChangeProcess > 0) {
                    Log.d(TAG, "mSpanChangeProcess > 0");
                    if (verticalScrollOffset > 0) {
                        int width = getItemSize(mCachedBorders, mSpanCount);
                        int toWidth = getItemSize(mToCachedBorders, mToSpanCount);
                        int count = getItemCount();
                        int newTotalHeight = count / mToSpanCount * toWidth;
                        if (count % mToSpanCount != 0)
                            newTotalHeight += toWidth;
                        int pheight = getHeight() - getPaddingBottom() - getPaddingTop();
                        if (verticalScrollOffset < newTotalHeight - pheight || newTotalHeight < pheight) {
                            if (newTotalHeight < pheight) {
                                Log.d(TAG, "newTotalHeight < pheight");
                                mOffsetDelta = (0.0f - verticalScrollOffset) / MAX_SPAN_CHANGE_PROCESS;

                            } else {
                                Log.d(TAG, "verticalScrollOffset < newTotalHeight - pheight");
                                int col = (int)verticalScrollOffset / width;
                                if (verticalScrollOffset % width != 0)
                                    ++col;
                                pheight /= 2;
                                col += (pheight / width);
                                if (pheight % width != 0)
                                    ++col;
                                int toOffset = col * toWidth - pheight;

                                mOffsetDelta = (1.0f * toOffset - verticalScrollOffset) / MAX_SPAN_CHANGE_PROCESS;
                            }
                        } else {
                            Log.d(TAG, "other");
                            mOffsetDelta = (1.0f * newTotalHeight - pheight - verticalScrollOffset) / MAX_SPAN_CHANGE_PROCESS;
                        }
                    } else {
                        Log.d(TAG, "0");
                        mOffsetDelta = 0;
                    }
                } else if (mSpanChangeProcess < 0) {

                    Log.d(TAG, "mSpanChangeProcess < 0");

                    int width = getItemSize(mCachedBorders, mSpanCount);
                    int toWidth = getItemSize(mToCachedBorders, mToSpanCount);
                    int count = getItemCount();
                    int newTotalHeight = count / mToSpanCount * toWidth;
                    if (count % mToSpanCount != 0)
                        newTotalHeight += toWidth;
                    int pheight = getHeight() - getPaddingBottom() - getPaddingTop();
                    int col =(int) verticalScrollOffset / width;
                    if (verticalScrollOffset % width != 0)
                        ++col;
                    pheight /= 2;
                    col += (pheight / width);
                    if (pheight % width != 0)
                        ++col;
                    int toOffset = col * toWidth - pheight;
                    mOffsetDelta = (1.0f * toOffset - verticalScrollOffset) / MAX_SPAN_CHANGE_PROCESS;


                }
                Log.d(TAG, "mOffsetDelta : "+mOffsetDelta);
            }

            //offsetChildrenVertical((int)(verticalScrollOffset+mOffsetDelta-verticalScrollOffset));
            verticalScrollOffset = (int) (initOffset+ mOffsetDelta*Math.abs(mSpanChangeProcess));
            Log.d(TAG, "verticalScrollOffset : "+verticalScrollOffset);
        }
        this.requestLayout();
    }

    public void startSpanChange() {
        isSpanChangeMode = true;
        this.mSpanChangeProcess = 0;
    }

    public void stopSpanChange() {
        if (isSpanChangeMode && mSpanChangeProcess != 0) {
            if (spanChangeAnim != null && spanChangeAnim.isRunning()) return;
            int toProcess = 0;
            if (mSpanChangeProcess > MAX_SPAN_CHANGE_PROCESS / 50 *12) {
                toProcess = MAX_SPAN_CHANGE_PROCESS;
            } else if (mSpanChangeProcess < - MAX_SPAN_CHANGE_PROCESS / 50 *12) {
                toProcess = -MAX_SPAN_CHANGE_PROCESS;

            }
            spanChangeAnim = ObjectAnimator.ofInt(this, "spanChangeProcess", mSpanChangeProcess, toProcess);
            spanChangeAnim.setDuration(20 / (MAX_SPAN_CHANGE_PROCESS / 50) * Math.abs(mSpanChangeProcess - toProcess));
            spanChangeAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            spanChangeAnim.start();
            spanChangeAnim.addListener(spanChangeListener);
        }
    }

    public CustomGridLayoutManager(int spanCount) {
        mSpanCount = spanCount;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() <= 0 || state.isPreLayout()) {
            return;
        }
        super.onLayoutChildren(recycler, state);
        detachAndScrapAttachedViews(recycler);
        calculateChildrenSite();
        recycleAndFillView(recycler, state);
    }

    static int[] calculateItemBorders(int[] cachedBorders, int spanCount, int totalSpace) {
        if (cachedBorders == null || cachedBorders.length != spanCount + 1 || cachedBorders[cachedBorders.length - 1] != totalSpace) {
            cachedBorders = new int[spanCount + 1];
        }
        cachedBorders[0] = 0;
        int sizePerSpan = totalSpace / spanCount;
        int sizePerSpanRemainder = totalSpace % spanCount;
        int consumedPixels = 0;
        int additionalSize = 0;

        for (int i = 1; i <= spanCount; ++i) {
            int itemSize = sizePerSpan;
            additionalSize += sizePerSpanRemainder;
            if (additionalSize > 0 && spanCount - additionalSize < sizePerSpanRemainder) {
                itemSize = sizePerSpan + 1;
                additionalSize -= spanCount;
            }
            consumedPixels += itemSize;
            cachedBorders[i] = consumedPixels;
        }

        return cachedBorders;
    }

    private int getItemSize(int[] cachedBorders, int spanCount) {
        if (spanCount > 1) {
            return cachedBorders[1] - cachedBorders[0];
        } else
            return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private void calculateItemBorders(int totalSpace) {
        this.mCachedBorders = calculateItemBorders(this.mCachedBorders, this.mSpanCount, totalSpace);
    }

    private void calculateChildrenSite() {
        totalHeight = 0;
        int totalSpace;
        totalSpace = this.getWidth() - this.getPaddingRight() - this.getPaddingLeft();
        if (mCachedBorders == null) {
            calculateItemBorders(totalSpace);
        }
        totalSpace = getItemSize(mCachedBorders, mSpanCount);
        Log.i(TAG, "totalSpace:" + totalSpace);

        for (int i = 0; i < getItemCount(); i++) {
            Rect rect = itemRects.get(i);
            if (rect == null) {
                rect = new Rect();
            }
            if (i % mSpanCount == 0 && i > 0)
                totalHeight += totalSpace;
            rect.set(mCachedBorders[i % mSpanCount], totalHeight, mCachedBorders[i % mSpanCount] + totalSpace, totalHeight + totalSpace);
            // 保存ItemView的位置信息
            itemRects.put(i, rect);

        }
        totalHeight += totalSpace;
        Log.d(TAG, "totalHeight:" + totalHeight);
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec, boolean alreadyMeasured) {
        android.support.v7.widget.RecyclerView.LayoutParams lp = (android.support.v7.widget.RecyclerView.LayoutParams) child.getLayoutParams();
        boolean measure;
        if (alreadyMeasured) {
            measure = this.shouldReMeasureChild(child, widthSpec, heightSpec, lp);
        } else {
            measure = this.shouldMeasureChild(child, widthSpec, heightSpec, lp);
        }
        if (measure) {
            child.measure(widthSpec, heightSpec);
        }
    }

    private void recycleAndFillView(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() <= 0 || state.isPreLayout()) {
            return;
        }
        // 当前scroll offset状态下的显示区域
        Rect displayRect = new Rect(0, (int)verticalScrollOffset, getHorizontalSpace(),
                (int)verticalScrollOffset + getVerticalSpace());
        int left, top, width, toWidth = 0;
        width = getItemSize(mCachedBorders, mSpanCount);
        if (mSpanChangeProcess != 0)
            toWidth = getItemSize(mToCachedBorders, mToSpanCount);
        Rect childRect = new Rect();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            childRect.left = getDecoratedLeft(child);
            childRect.top = getDecoratedTop(child);
            childRect.right = getDecoratedRight(child);
            childRect.bottom = getDecoratedBottom(child);
            if (mSpanChangeProcess != 0) {
                left = mToCachedBorders[i % mToSpanCount];
                top = i / mToSpanCount * toWidth;
                int process = Math.abs(mSpanChangeProcess);
                childRect.left = childRect.left + (int) (1.0f * (left - childRect.left) / MAX_SPAN_CHANGE_PROCESS * process);
                childRect.top = childRect.top + (int) (1.0f * (top - childRect.top) / MAX_SPAN_CHANGE_PROCESS * process);
                int w = (int) (width + (1.0f * (toWidth - width) / MAX_SPAN_CHANGE_PROCESS) * process);
                childRect.right = childRect.left + w;
                childRect.bottom = childRect.top + w;
            }
            if (!Rect.intersects(displayRect, childRect)) {
                //recycle View
                removeAndRecycleView(child, recycler);
            }
        }
        for (int i = 0; i < getItemCount(); i++) {
            childRect = itemRects.get(i);
            if (mSpanChangeProcess != 0) {
                childRect = new Rect(childRect.left, childRect.top, childRect.right, childRect.bottom);
                left = mToCachedBorders[i % mToSpanCount];
                top = i / mToSpanCount * toWidth;
                int process = Math.abs(mSpanChangeProcess);
                childRect.left = childRect.left + (int) (1.0f * (left - childRect.left) / MAX_SPAN_CHANGE_PROCESS * process);
                childRect.top = childRect.top + (int) (1.0f * (top - childRect.top) / MAX_SPAN_CHANGE_PROCESS * process);
                int w = (int) (width + (1.0f * (toWidth - width) / MAX_SPAN_CHANGE_PROCESS) * process);
                childRect.right = childRect.left + w;
                childRect.bottom = childRect.top + w;

            }
            if (Rect.intersects(displayRect, childRect)) {
                View itemView = recycler.getViewForPosition(i);
                int spec = View.MeasureSpec.makeMeasureSpec(childRect.right - childRect.left, View.MeasureSpec.EXACTLY);
                measureChildWithDecorationsAndMargin(itemView, spec, spec, false);

                addView(itemView);

                layoutDecoratedWithMargins(itemView,
                        childRect.left,
                        childRect.top - (int)verticalScrollOffset,
                        childRect.right,
                        childRect.bottom - (int)verticalScrollOffset);
            }
        }
        Log.e(TAG, "itemCount = " + getChildCount());
    }


    @Override
    public boolean canScrollVertically() {
        //spanchange cannot scroll
        return !isSpanChangeMode;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        //不满一屏无法滚动
        Log.d(TAG, "totalHeight" + totalHeight + " dy:" + dy + " height:" + getVerticalSpace() + " verticalScrollOffset:" + verticalScrollOffset);
        if (totalHeight < getVerticalSpace()) {
            recycleAndFillView(recycler, state);
            return 0;
        }
        int travel = dy;

        if (verticalScrollOffset + dy < 0) {
            travel = -(int)verticalScrollOffset;
        } else if (verticalScrollOffset + dy > totalHeight - getVerticalSpace()) {
            travel = totalHeight - getVerticalSpace() - (int)verticalScrollOffset;
        }
        verticalScrollOffset += travel;
        offsetChildrenVertical(-travel);
        recycleAndFillView(recycler, state);
        Log.d(TAG, "verticalScrollOffset:" + verticalScrollOffset + " travel:" + travel);
        return travel;
    }

    private int getVerticalSpace() {

        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    @Override
    public boolean canScrollHorizontally() {

        return super.canScrollHorizontally();
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        return super.scrollHorizontallyBy(dx, recycler, state);
    }

    public int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }
}