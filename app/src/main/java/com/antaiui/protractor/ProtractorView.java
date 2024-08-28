package com.antaiui.protractor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.LongDef;


public class ProtractorView extends View {
    private PointF mCenterPoint;
    private Paint mPaint;
    private PointF mEndPoint1;
    private PointF mEndPoint2;
    private PointF mPointLeft;
    private static final float MOVE_DISTANCE = 100;
    private int distance = 600;//debug线区域的长度
    private float mDegree = 0f;//回调的角度

    //这里不写90有坑的
    private float mDegreeL = 90f;//初始角度
    private float mDegreeR = 90f;//初始角度
    private Bitmap bitmap;
    private Bitmap mCenterDotBitmap;
    private boolean mDebug = true;
    int moveType = 0;
    private String TAG = "ProtractorView";
    private MoveAngleCallBack mMoveAngleCallBack;
    private int mPaddingBottom = 30;

    public ProtractorView(Context context) {
        super(context);
    }

    public void setMoveAngleCallBack(MoveAngleCallBack callBack) {
        mMoveAngleCallBack = callBack;
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public ProtractorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

    }

    @Override
    public void draw(Canvas canvas) {
        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setStrokeWidth(5);
            mPaint.setColor(Color.RED);
            mPaint.setAntiAlias(true);
            //横版的话 左上角->竖着拿的右上角  才是【0.0】,当前坐标系【4x2】
            //[2,2]
            mCenterPoint = new PointF(0, getHeight() /2f);
            //[0,2]，左下角的位置
            mPointLeft = new PointF(0, 0);

            //TODO 如果想改初始红线位置改这里
            //现在这种坐标，x保持中点不变，通过减法distance 往上连线得到，俩先设置一样，如果
            mEndPoint1 = new PointF(mCenterPoint.x+ distance, mCenterPoint.y );
            mEndPoint2 = new PointF(mCenterPoint.x+ distance, mCenterPoint.y );


            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pointer_icon, options);
            Matrix matrix = new Matrix();
            //让图片转到正确角度，如果切图是横着的，可以调这个来实现效果
            matrix.postRotate(-90, bitmap.getWidth(), bitmap.getHeight());
            Bitmap rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = rotateBitmap;

            mCenterDotBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.center_dot_icon, options);
            Log.d(TAG, "draw center = " + mCenterPoint.toString() + " end = " + mEndPoint1.toString());
        }


        //画红线位置，只要点准线就准
        if (mDebug) {
            canvas.drawLine(mCenterPoint.x, mCenterPoint.y, mEndPoint1.x, mEndPoint1.y, mPaint);
            canvas.drawLine(mCenterPoint.x, mCenterPoint.y, mEndPoint2.x, mEndPoint2.y, mPaint);
        }
        Paint fanPaint = new Paint();
        fanPaint.setColor(Color.BLUE);


        RectF oval = new RectF(mCenterPoint.x - distance, mCenterPoint.y - distance, mCenterPoint.x + distance, mCenterPoint.y + distance);
        //mDegreeL，第一个参数需要动态调整，他的初始角度是-90，原因未知
        canvas.drawArc(oval, mDegreeL -90f, mDegreeR - mDegreeL, true, fanPaint); // 绘制扇形


        Matrix matrix = new Matrix();
        int offsetX = bitmap.getWidth();
        int offsetY = bitmap.getHeight();
        matrix.preTranslate(mCenterPoint.x - offsetX, mCenterPoint.y - offsetY / 2f);
        matrix.postRotate(mDegreeL+ 90f, mCenterPoint.x, mCenterPoint.y);
        canvas.drawBitmap(bitmap, matrix, mPaint);

        Matrix matrixR = new Matrix();
        matrixR.preTranslate(mCenterPoint.x - offsetX, mCenterPoint.y - offsetY / 2f);
        matrixR.postRotate(mDegreeR+90f, mCenterPoint.x, mCenterPoint.y);
        canvas.drawBitmap(bitmap, matrixR, mPaint);


        int width = mCenterDotBitmap.getWidth();
        int height = mCenterDotBitmap.getHeight();
        canvas.drawBitmap(mCenterDotBitmap, mCenterPoint.x - width / 2, mCenterPoint.y - height / 2, mPaint);

        super.draw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                computerMoveDirection(new PointF(event.getX(), event.getY()));
                break;
            case MotionEvent.ACTION_MOVE:
                PointF pointF = new PointF(event.getX(), event.getY());
                computerAngle(pointF);
                break;
            default:
                break;
        }

        return true;
    }


    private void computerAngle(PointF pointF) {
        if (moveType == 1) {
            mEndPoint1 = computerEndPoint(pointF);
            //【俩目标线夹角】中点，目标点1；目标点二
            mDegree = computerAngle(mCenterPoint.x, mCenterPoint.y, mEndPoint1.x, mEndPoint1.y, mEndPoint2.x, mEndPoint2.y);
            //【最左端和线1夹角】中点，目标点1，左下角；
            mDegreeL = computerAngle(mCenterPoint.x, mCenterPoint.y, mEndPoint1.x, mEndPoint1.y, mPointLeft.x, mPointLeft.y);
            //加个限制，转到0或180 会影响到会弧形 mDegreeL=NaN


            invalidate();
            mMoveAngleCallBack.angleCallBack(mDegree);
        } else if (moveType == 2) {
            mEndPoint2 = computerEndPoint(pointF);
            //中点，目标点1；目标点二
            mDegree = computerAngle(mCenterPoint.x, mCenterPoint.y, mEndPoint1.x, mEndPoint1.y, mEndPoint2.x, mEndPoint2.y);
            //【最左端和线2夹角】中点，左下角位置；目标点二
            mDegreeR = computerAngle(mCenterPoint.x, mCenterPoint.y, mPointLeft.x, mPointLeft.y, mEndPoint2.x, mEndPoint2.y);
            invalidate();
            mMoveAngleCallBack.angleCallBack(mDegree);
        }

    }


    //按下时将点定位
    private PointF computerEndPoint(PointF movePoint) {
        //欧斯公式计算 movePoint 到一个叫 mCenterPoint 的点的直线距离
        double distanceToCenter = Math.sqrt(Math.pow(movePoint.x - mCenterPoint.x, 2)
                + Math.pow(movePoint.y - mCenterPoint.y, 2));
        //圆心到movepoint垂直距离：(getHeight() - movePoint.y)
        double sin = ( movePoint.x) / distanceToCenter;
        //圆心到 movePoint 的水平距离（movePoint.x - mCenterPoint.x） 这个用小减大
        double cos = ( movePoint.y-mCenterPoint.y) / distanceToCenter;


        PointF result = new PointF((float) (mCenterPoint.x + sin * distance),
                (float) ( mCenterPoint.y+distance * cos));
        //设定边界 防出界面
        if (result.x < mCenterPoint.x) {
            result.x = mCenterPoint.x;
        }

        return result;
    }



    //computer∠yxz angle 三个二维点定角度  对应A,B,C,
    private float computerAngle(float x1, float y1, float x2, float y2, float x3, float y3) {
        // 计算向量 AB 和 AC
        float abX = x2 - x1;
        float abY = y2 - y1;
        float acX = x3 - x1;
        float acY = y3 - y1;

        // 向量的点乘
        float dotProduct = abX * acX + abY * acY;

        // 计算向量 AB 和 AC 的模长
        float abLength = (float) Math.sqrt(abX * abX + abY * abY);
        float acLength = (float) Math.sqrt(acX * acX + acY * acY);

        // 检查向量长度是否为零以避免除以零的情况
        if (abLength == 0 || acLength == 0) {
            throw new IllegalArgumentException("输入的点不能重合，导致向量长度为零。");
        }

        // 计算余弦值
        float cosAngle = dotProduct / (abLength * acLength);

        // 确保余弦值在[-1, 1]之间，防止 acos 产生 NaN
        cosAngle = Math.max(-1, Math.min(1, cosAngle));
        // 使用反余弦函数计算角度，并转换为度
        float angleInRadians = (float) Math.acos(cosAngle);
        float angleInDegrees = (float) (angleInRadians * 180 / Math.PI);

        return angleInDegrees;
    }



    public interface MoveAngleCallBack {
        public void angleCallBack(float angle);
    }

    private void computerMoveDirection(PointF downPoint) {
        moveType = 0;
        double distanceToLine1 = pointToLine(mCenterPoint, mEndPoint1, downPoint);
        double distanceToLine2 = pointToLine(mCenterPoint, mEndPoint2, downPoint);
        if (distanceToLine1 < MOVE_DISTANCE) {
            moveType = 1;
        }
        if (distanceToLine1 > distanceToLine2 && distanceToLine2 < MOVE_DISTANCE) {
            moveType = 2;
        }
    }

    // 点到直线的最短距离的判断 点（x0,y0） 到由两点组成的线段（x1,y1） ,( x2,y2 )
    private double pointToLine(PointF centerPoint, PointF endPoint, PointF downPoint) {
        double space = 0;
        double a, b, c;
        a = lineSpace(centerPoint, endPoint);// 线段的长度
        b = lineSpace(centerPoint, downPoint);// (x1,y1)到点的距离
        c = lineSpace(endPoint, downPoint);// (x2,y2)到点的距离
        if (c <= 0.000001 || b <= 0.000001) {
            space = 0;
            return space;
        }
        if (a <= 0.000001) {
            space = b;
            return space;
        }
        if (c * c >= a * a + b * b) {
            space = b;
            return space;
        }
        if (b * b >= a * a + c * c) {
            space = c;
            return space;
        }
        double p = (a + b + c) / 2;// 半周长
        double s = Math.sqrt(p * (p - a) * (p - b) * (p - c));// 海伦公式求面积
        space = 2 * s / a;// 返回点到线的距离（利用三角形面积公式求高）
        return space;
    }

    // 计算两点之间的距离
    private double lineSpace(PointF point1, PointF point2) {
        double lineLength = 0;
        lineLength = Math.sqrt((point1.x - point2.x) * (point1.x - point2.x) + (point1.y - point2.y)
                * (point1.y - point2.y));
        return lineLength;
    }

}
