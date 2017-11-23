#include <jni.h>
#include <string>
#include "include/opencv2/opencv.hpp"
#include "include/opencv2/core/core.hpp"
#include "include/opencv2/core.hpp"
#include "include/opencv2/core/base.hpp"
#include "include/opencv2/core/cvdef.h"
#include "include/opencv2/calib3d.hpp"
#include "include/opencv2/imgproc/types_c.h"
#include "include/opencv2/core/types.hpp"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <../include/opencv2/highgui/highgui.hpp>
#include <../include/opencv2/imgproc/imgproc.hpp>
#include <vector>
using namespace cv;
using namespace std;
extern "C"
JNIEXPORT jobject
JNICALL
Java_com_water_seed_MainActivity_getPointArray(
        JNIEnv *env, jclass clz, jintArray pixels, jint width, jint height) {
    jint *buff = env->GetIntArrayElements(pixels, (jboolean*) false);
    if (buff == NULL) {
        return 0;
    }

    Mat src(height, width, CV_8UC4, buff);
    Mat gray;
    cvtColor(src, gray, COLOR_BGR2GRAY); //彩色图像转换为灰度图像
    Mat bw;
    threshold(gray, bw, 30, 255, CV_THRESH_OTSU|THRESH_BINARY_INV); //灰度图像转换为黑白二值图像

    Mat kernel = getStructuringElement(MORPH_RECT, Size(7, 7));
    erode(bw, bw, kernel);
    Mat tmp;
    bw.copyTo(tmp);
    vector<vector<Point> > contours;
    //获取轮廓
    findContours(tmp, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE); //进行腐蚀，缩小目标

    Mat dstImg=Mat::zeros(height,width,CV_8U);
    Mat dist;
    Mat norm;
    Mat thre;
    Mat dist_8u;
    for(int i = 0; i < contours.size(); i++)
    {
        Rect rect=boundingRect(contours[i]);
        Mat roiImg=bw(rect);
        Mat newImg=Mat::zeros(rect.size(),CV_8UC1);
        drawContours(newImg,contours,i,Scalar(255),CV_FILLED,8,noArray(),INT_MAX,Point(-rect.x,-rect.y));
        bitwise_and(roiImg,newImg,newImg);

        distanceTransform(newImg, dist, CV_DIST_L2, 3); //得到距离变换图像
        normalize(dist, norm, 0, 1., NORM_MINMAX); //对距离变换图像进行归一化
        threshold(norm, thre,0.5, 255, CV_THRESH_BINARY); //对归一化图进行二值化
        thre.convertTo(dist_8u, CV_8U); //转换成8位图
        bitwise_or(dstImg(rect),dist_8u,dstImg(rect));
    }

    findContours(dstImg, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE); //进行腐蚀，缩小目标
    Point2f center;
    float radius;
    jclass list_cls = env->FindClass("java/util/ArrayList"); //获得ArrayList类引用
    jmethodID list_costruct = env->GetMethodID(list_cls, "<init>", "()V"); //获得得构造函数Id
    jobject list_obj = env->NewObject(list_cls, list_costruct); //创建一个Arraylist集合对象
    //或得Arraylist类中的 add()方法ID，其方法原型为： boolean add(Object object) ;
    jmethodID list_add = env->GetMethodID(list_cls, "add",
                                          "(Ljava/lang/Object;)Z");
    jclass stu_cls = env->FindClass("android/graphics/Point"); //获得Point类引用
    jmethodID stu_costruct = env->GetMethodID(stu_cls, "<init>", "(II)V");
    for (int i = 0; i < contours.size(); i++) { //通过调用该对象的构造函数来new 一个 Student实例
        if(contours[i].size()<6)continue;
        minEnclosingCircle(contours[i], center, radius); //这里可以得到一系列坐标点
        jobject stu_obj = env->NewObject(stu_cls, stu_costruct, (int) center.x,
                                         (int) center.y); //构造一个对象
        env->CallBooleanMethod(list_obj, list_add, stu_obj); //执行Arraylist类实例的add方法，添加一个stu对象
        //打印出来看看
        env->DeleteLocalRef(stu_obj);

    }
    vector<vector<Point> >().swap(contours);
    env->DeleteLocalRef(stu_cls);
    env->DeleteLocalRef(list_cls);
    env->ReleaseIntArrayElements(pixels, buff, 0);
    return list_obj;
}
