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

    public static final int MAX_SPAN_CHANGE_PROCESS = 500;
    //private SparseArray<Rect> itemRects = new SparseArray<>();
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
        if (!isSpanChangeMode) {
            Log.i(TAG, "must call in spanCountChange mode");
            return;
        }
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

    public void incChangeProcess() {
        if (!isSpanChangeMode) {
            Log.i(TAG, "must call in spanCountChange mode");
            return;
        }
        this.mSpanChangeProcess++;
        if (mSpanChangeProcess > MAX_SPAN_CHANGE_PROCESS)
            mSpanChangeProcess = MAX_SPAN_CHANGE_PROCESS;
        Log.d(TAG, "spanChangeProcess:" + mSpanChangeProcess);
        updateSpanChangeProcess();
    }

    public void reduceChangeProcess() {
        if (!isSpanChangeMode) {
            Log.i(TAG, "must call in spanCountChange mode");
            return;
        }
        this.mSpanChangeProcess--;
        if (mSpanChangeProcess < MAX_SPAN_CHANGE_PROCESS)
            mSpanChangeProcess = -MAX_SPAN_CHANGE_PROCESS;
        updateSpanChangeProcess();
    }

    private void calcOffset() {
        if (verticalScrollOffset > 0) {
            int width = getItemSize(mCachedBorders, mSpanCount);
            int toWidth = getItemSize(mToCachedBorders, mToSpanCount);
            int count = getItemCount();
            int newTotalHeight = count / mToSpanCount * toWidth;
            if (count % mToSpanCount != 0)
                newTotalHeight += toWidth;
            int pheight = getHeight() - getPaddingBottom() - getPaddingTop();

            if (newTotalHeight <= pheight) {
                Log.d(TAG, "newTotalHeight <= pheight");
                mOffsetDelta = (0.0f - verticalScrollOffset) / MAX_SPAN_CHANGE_PROCESS;

            } else {
                Log.d(TAG, "newTotalHeight > pheight");
                int col = (verticalScrollOffset+pheight/2) / width;
                Log.d(TAG,"mid:"+(col * mSpanCount + mSpanCount / 2));
                col = (col * mSpanCount + mSpanCount / 2) / mToSpanCount;

                int toOffset = col * toWidth - pheight/2;
                if (toOffset <= 0)
                    mOffsetDelta = (0.0f - verticalScrollOffset) / MAX_SPAN_CHANGE_PROCESS;
                else if (toOffset <= newTotalHeight - pheight) {
                    Log.d(TAG, "toOffset<=newTotalHeight-getVerticalSpace()");
                    mOffsetDelta = (1.0f * toOffset - verticalScrollOffset) / MAX_SPAN_CHANGE_PROCESS;
                } else {
                    Log.d(TAG, "other");
                    mOffsetDelta = (1.0f * newTotalHeight - pheight - verticalScrollOffset) / MAX_SPAN_CHANGE_PROCESS;

                }
            }

        } else {
            Log.d(TAG, "0");
            mOffsetDelta = 0;
        }

        Log.d(TAG, "mOffsetDelta : " + mOffsetDelta);

    }

    private void updateSpanChangeProcess() {
        if (!isSpanChangeMode) {
            Log.i(TAG, "must call in spanCountChange mode");
            return;
        }
        Log.d(TAG, "mSpanChangeProcess:" + this.mSpanChangeProcess);
        if (this.mSpanChangeProcess == MAX_SPAN_CHANGE_PROCESS||this.mSpanChangeProcess == -MAX_SPAN_CHANGE_PROCESS) {
            if(this.mSpanChangeProcess==MAX_SPAN_CHANGE_PROCESS)
                mSpanCount += 2;
            else
                mSpanCount-=2;
            mCachedBorders = null;
            mToCachedBorders = null;
            verticalScrollOffset = (int) (initOffset + mOffsetDelta * Math.abs(mSpanChangeProcess));
            mSpanChangeProcess = 0;
            isSpanChangeMode = false;
            Log.d(TAG, "verticalScrollOffset:" + verticalScrollOffset);
            calculateChildrenSite();
        }  else {
            if ((mSpanCount <= 2 && this.mSpanChangeProcess < 0) || (mSpanCount > 8 && this.mSpanChangeProcess > 0)) {
                mSpanChangeProcess = 0;
                isSpanChangeMode = false;
                return;
            }
            if (mSpanChangeProcess == 0||(mToSpanCount-mSpanCount)*mSpanChangeProcess<=0)
                mToCachedBorders = null;
            if (mToCachedBorders == null) {
                if (mSpanChangeProcess > 0)
                    mToSpanCount = mSpanCount + 2;
                else if (mSpanChangeProcess < 0)
                    mToSpanCount = mSpanCount - 2;
                else {
                    mToSpanCount = mSpanCount;
                    return;
                }
                int totalSpace;
                totalSpace = this.getWidth() - this.getPaddingRight() - this.getPaddingLeft();
                mToCachedBorders = calculateItemBorders(mToCachedBorders, mToSpanCount, totalSpace);
                //当发生偏移的时候，因为itemSize发生变化，为了显示当前画面，需要更新滑动距离
                calcOffset();
            }
            //offsetChildrenVertical((int)(verticalScrollOffset+mOffsetDelta-verticalScrollOffset));
            verticalScrollOffset = (int) (initOffset + mOffsetDelta * Math.abs(mSpanChangeProcess));
            Log.d(TAG, "verticalScrollOffset : " + verticalScrollOffset);
        }
        this.requestLayout();
    }

    public void startSpanChange() {
        isSpanChangeMode = true;
        this.mSpanChangeProcess = 0;
        initOffset = verticalScrollOffset;
        mToSpanCount=mSpanCount;
    }

    public void stopSpanChange() {
        if (isSpanChangeMode && mSpanChangeProcess != 0) {
            if (spanChangeAnim != null && spanChangeAnim.isRunning()) return;
            int toProcess = 0;
            if (mSpanChangeProcess > MAX_SPAN_CHANGE_PROCESS / 50 * 12) {
                toProcess = MAX_SPAN_CHANGE_PROCESS;
            } else if (mSpanChangeProcess < -MAX_SPAN_CHANGE_PROCESS / 50 * 12) {
                toProcess = -MAX_SPAN_CHANGE_PROCESS;

            }
            spanChangeAnim = ObjectAnimator.ofInt(this, "spanChangeProcess", mSpanChangeProcess, toProcess);
            spanChangeAnim.setDuration(10 / (MAX_SPAN_CHANGE_PROCESS / 50) * Math.abs(mSpanChangeProcess - toProcess));
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

//        for (int i = 0; i < getItemCount(); i++) {
//            Rect rect = itemRects.get(i);
//            if (rect == null) {
//                rect = new Rect();
//            }
//            if (i % mSpanCount == 0 && i > 0)
//                totalHeight += totalSpace;
//            rect.set(mCachedBorders[i % mSpanCount], totalHeight, mCachedBorders[i % mSpanCount] + totalSpace, totalHeight + totalSpace);
//            // 保存ItemView的位置信息
//            itemRects.put(i, rect);
//
//        }
        int count=getItemCount();
        totalHeight =count/mSpanCount*totalSpace;
        if(count%mSpanCount!=0)
            totalHeight+=totalSpace;
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
        Rect displayRect = new Rect(0, (int) verticalScrollOffset, getHorizontalSpace(),
                (int) verticalScrollOffset + getVerticalSpace());
        int left, top, width, toWidth = 0;
        width = getItemSize(mCachedBorders, mSpanCount);
        int w=width;
        int process = Math.abs(mSpanChangeProcess);
        if (mSpanChangeProcess != 0){
            toWidth = getItemSize(mToCachedBorders, mToSpanCount);
            w = (int) (width + (1.0f * (toWidth - width) / MAX_SPAN_CHANGE_PROCESS) * process);
        }
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
                childRect.left = childRect.left + (int) (1.0f * (left - childRect.left) / MAX_SPAN_CHANGE_PROCESS * process);
                childRect.top = childRect.top + (int) (1.0f * (top - childRect.top) / MAX_SPAN_CHANGE_PROCESS * process);
                childRect.right = childRect.left + w;
                childRect.bottom = childRect.top + w;
            }
            if (!Rect.intersects(displayRect, childRect)) {
                //recycle View
                removeAndRecycleView(child, recycler);
            }
        }
        boolean alreadyShow=false;
        //只从当前显示区域上方开始判断
        int showOffset =verticalScrollOffset-5*w;
        if(showOffset<0)
            showOffset=0;
        if(mSpanChangeProcess!=0)
            showOffset=showOffset/w*Math.min(mSpanCount,mToSpanCount);
        else
            showOffset=showOffset/w*mSpanCount;
        for (int i = showOffset; i < getItemCount(); i++) {
            left=mCachedBorders[i%mSpanCount];
            top=i/mSpanCount*width;
            childRect = new Rect(left,top,left+width,top+width);
            if (mSpanChangeProcess != 0) {
                childRect = new Rect(childRect.left, childRect.top, childRect.right, childRect.bottom);
                left = mToCachedBorders[i % mToSpanCount];
                top = i / mToSpanCount * toWidth;
                childRect.left = childRect.left + (int) (1.0f * (left - childRect.left) / MAX_SPAN_CHANGE_PROCESS * process);
                childRect.top = childRect.top + (int) (1.0f * (top - childRect.top) / MAX_SPAN_CHANGE_PROCESS * process);
                childRect.right = childRect.left + w;
                childRect.bottom = childRect.top + w;

            }
            if (Rect.intersects(displayRect, childRect)) {
                alreadyShow=true;
                View itemView = recycler.getViewForPosition(i);
                int spec = View.MeasureSpec.makeMeasureSpec(childRect.right - childRect.left, View.MeasureSpec.EXACTLY);
                measureChildWithDecorationsAndMargin(itemView, spec, spec, false);

                addView(itemView);

                layoutDecoratedWithMargins(itemView,
                        childRect.left,
                        childRect.top - (int) verticalScrollOffset,
                        childRect.right,
                        childRect.bottom - (int) verticalScrollOffset);
            }else if(alreadyShow)
                break;
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
            travel = -(int) verticalScrollOffset;
        } else if (verticalScrollOffset + dy > totalHeight - getVerticalSpace()) {
            travel = totalHeight - getVerticalSpace() - (int) verticalScrollOffset;
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
    public static int scaleToProcess(float scale){
        if(scale==1)
            return 0;
        else if(scale>1){
            return -(int)((scale-1)*200);
        }else
            return (int)((1-scale)*1000);
    }
}