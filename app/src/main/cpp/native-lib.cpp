#include <jni.h>
#include <string.h>
#include <opencv2/opencv.hpp>
#include <ctime>
#include <cstdlib>
#include <android/log.h>
#include <math.h>
#include <unistd.h>
#include <assert.h >
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>


#include "uface_types.h"
#include<android/log.h>
#include "uliveness.h"
#include "uliveness_types.h"
//#include "file/file.h"


// log标签
#define  TAG    "native-lib"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
// 定义debug信息
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
// 定义error信息
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

using namespace std;
using namespace cv;

//人脸检测器
static cv::CascadeClassifier *face_detecter = nullptr;


extern "C"
JNIEXPORT jstring

JNICALL
Java_cc_uphoton_bctc_activity_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello I am from C++";
    return env->NewStringUTF(hello.c_str());
}
//,jstring out_put_file_path

extern "C"
JNIEXPORT void
        JNICALL
Java_cc_uphoton_bctc_activity_MainActivity_CopyMnnData(JNIEnv
* env,
jclass type, jobject
assetManager,
jstring filename, jstring
out_put_file_path
){

AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);


/*获取文件名并打开*/
jboolean iscopy;
const char *mfile = env->GetStringUTFChars(filename, 0);
const char *mOutPutFilePath = env->GetStringUTFChars(out_put_file_path, 0);
AAsset *asset = AAssetManager_open(mgr, mfile, AASSET_MODE_UNKNOWN);


/*获取文件大小*/
off_t bufferSize = AAsset_getLength(asset);
char *buffer = (char *) malloc(bufferSize + 1);
// buffer[bufferSize]=0;
int numBytesRead = AAsset_read(asset, buffer, bufferSize);
//FILE *outfile = fopen("/data/data/cc.uphoton.bctc/files/ulivenet_ir.mnn", "wb+");
FILE *outfile = fopen(mOutPutFilePath, "wb+");
fwrite(buffer, numBytesRead,
1,outfile);
free(buffer);
fclose(outfile);
/*关闭文件*/
AAsset_close(asset);
env->
ReleaseStringUTFChars( filename, mfile
);
env->
ReleaseStringUTFChars( out_put_file_path, mOutPutFilePath
);

}




extern "C"
JNIEXPORT void
        JNICALL
Java_cc_uphoton_bctc_activity_MainActivity_initLiveness(JNIEnv
* env,
jclass
        type,
        jstring
ref_path,
jstring
        ymlPath
){
//读取P矩阵
cv::Mat P;
const char *nativepath = env->GetStringUTFChars(ymlPath, 0);
cv::FileStorage storage(nativepath, cv::FileStorage::READ);
P = storage["P"].mat();
storage.

release();

env->
ReleaseStringUTFChars(ymlPath, nativepath
);

//读取参考图
const char *refPath = env->GetStringUTFChars(ref_path, 0);
//读取模型数据
//const char *modelPath = env->GetStringUTFChars(model_path, 0);
//const char *face_modelPath = env->GetStringUTFChars(face_model_path, 0);


// 设置参数
// 深度图初始化参数
struct uphoton::udepthmap::InitParams depth_init_params;
depth_init_params.
reference_depth = 50;              // 设置参考图距离，单位cm，根据需要修改
depth_init_params.
focal_length = 2.74f;              // ir摄像头焦距，单位mm
depth_init_params.
base_width = 40.0f;                // 基线距离，单位mm
depth_init_params.
pixel_size = 0.0054f;              // 像素大小，单位mm
depth_init_params.
depth_detection_min = 26;          // 设置的最小检测距离，单位cm
depth_init_params.
depth_detection_max = 100;         // 设置的最大检测距离，单位cm
depth_init_params.
disparity_period_DOE = 60;
depth_init_params.
horizontal_flip = true;

uphoton::uliveness::UFaceMaskConfig face_mask_config;
face_mask_config.
depth_init_params = depth_init_params;
face_mask_config.
P = P;
face_mask_config.
direction = uphoton::ucalibrate::VERTICAL;
face_mask_config.
face_type = uphoton::uface::UPHOTON_FACE_TYPE;
//face_mask_config.face_model_path = face_modelPath;
face_mask_config.
reference_image_path = refPath;

uphoton::uliveness::ULiveNetConfig live_net_config;
live_net_config.
power = uphoton::uliveness::POWER_NORMAL;
live_net_config.
memory = uphoton::uliveness::MEMORY_NORMAL;
live_net_config.
precision = uphoton::uliveness::PRECISION_LOW;
live_net_config.
number_thread = 4;
//live_net_config.model_path = modelPath;
// 初始化
uphoton::uliveness::Init(face_mask_config, live_net_config
);

env->
ReleaseStringUTFChars(ref_path, refPath
);
/*env->
ReleaseStringUTFChars(model_path, modelPath
);
env->
ReleaseStringUTFChars(face_model_path, face_modelPath
);*/
}

extern "C"
JNIEXPORT jobject
JNICALL
        Java_cc_uphoton_bctc_activity_MainActivity_FaceDetectLivenessFromByte(JNIEnv * env, jclass
type,
jbyteArray imgObj, jbyteArray
imgRGB,
jfloat default_liveness
){
jbyte *obj_native_data = env->GetByteArrayElements(imgObj, 0);
jbyte *rgb_native_data = env->GetByteArrayElements(imgRGB, 0);
//16位单通道
//cv::Mat mat = cv::Mat(640, 400, CV_16UC1, (void *)obj_native_data);
//8位3通道

cv::Mat object_image = cv::Mat(640, 400, CV_8UC3, (void *) obj_native_data);
cv::Mat rgb_image = cv::Mat(640, 480, CV_8UC3, (void *) rgb_native_data);

cv::flip(rgb_image, rgb_image, 1);

cvtColor(rgb_image, rgb_image, COLOR_RGB2BGR);
//cv::imwrite("/storage/emulated/0/USBBCTC/rgb_src.jpg",rgb_image);
//cv::imwrite("/storage/emulated/0/USBBCTC/ir_src.jpg",object_image);

uphoton::uliveness::ULivenessReport uliveness_report;
// 设置计算深度图的参数
uphoton::udepthmap::CalculateSettings depth_settings;
uphoton::uliveness::FaceInputTypes face_input_source = uphoton::uliveness::IR_IMAGE_ONLY;

uint64_t start_time, end_time;
struct timeval tv;
gettimeofday(&tv,
nullptr);
start_time = static_cast<uint64_t>(tv.tv_sec) * 1000000 + tv.tv_usec;

uphoton::uliveness::GetLivenessByFaceDetect(rgb_image, object_image, uliveness_report, face_input_source);
//cv::imwrite("/storage/emulated/0/USBBCTC/rgb_face.jpg",uliveness_report.rgb_face);
//cv::imwrite("/storage/emulated/0/USBBCTC/ir_face.jpg",uliveness_report.ir_speckle_face);

gettimeofday(&tv,
nullptr);
end_time = static_cast<uint64_t>(tv.tv_sec) * 1000000 + tv.tv_usec;
double time = (end_time - start_time) / 1000.0;


jclass livenessResultBean = env->FindClass("cc/uphoton/bctc/bean/LivenessResultBean");
jfieldID jDepthFull = env->GetFieldID(livenessResultBean, "depth_full", "[I");
jfieldID jDepthFace = env->GetFieldID(livenessResultBean, "depth_face", "[I");
jfieldID jIRFace = env->GetFieldID(livenessResultBean, "ir_face", "[I");
jfieldID jRGBFace = env->GetFieldID(livenessResultBean, "rgb_face", "[I");
jfieldID jLiveness_probs = env->GetFieldID(livenessResultBean, "livness_probs", "[F");
jfieldID jFace_bbox_ir = env->GetFieldID(livenessResultBean, "face_bbox_ir", "[I");
jfieldID jDepth_width = env->GetFieldID(livenessResultBean, "depth_width", "I");
jfieldID jDepth_height = env->GetFieldID(livenessResultBean, "depth_height", "I");
jfieldID jIR_height = env->GetFieldID(livenessResultBean, "ir_height", "I");
jfieldID jIR_width = env->GetFieldID(livenessResultBean, "ir_width", "I");
jfieldID jRGB_width = env->GetFieldID(livenessResultBean, "rgb_width", "I");
jfieldID jRGB_height = env->GetFieldID(livenessResultBean, "rgb_height", "I");
jfieldID jHasFaceFlag = env->GetFieldID(livenessResultBean, "has_face_flag", "I");
jfieldID jCost_time = env->GetFieldID(livenessResultBean, "cost_time", "D");
jfieldID jFace_net_time_only = env->GetFieldID(livenessResultBean, "face_net_time_only", "F");
jfieldID jFace_detect_time_only = env->GetFieldID(livenessResultBean, "face_detect_time", "F");
jfieldID jLive_net_time_only = env->GetFieldID(livenessResultBean, "live_net_time_only", "F");
jfieldID jLive_detect_time = env->GetFieldID(livenessResultBean, "live_detect_time", "F");


//创建新的对象
jobject jobj = env->AllocObject(livenessResultBean);
env->
SetDoubleField(jobj, jCost_time, time
);


env->SetIntField(jobj, jHasFaceFlag, uliveness_report
.has_face_flag);
if(uliveness_report.has_face_flag==1){
//返回数组
jfloatArray liveness_probs_array = env->NewFloatArray(2);
env->
SetFloatArrayRegion(liveness_probs_array,
0,2,uliveness_report.livness_probs);
env->
SetObjectField(jobj, jLiveness_probs, liveness_probs_array
);
int face_box_ir_result[4];
face_box_ir_result[0]=uliveness_report.face_bbox_ir.
x;
face_box_ir_result[1]=uliveness_report.face_bbox_ir.
y;
face_box_ir_result[2]=uliveness_report.face_bbox_ir.
width;
face_box_ir_result[3]=uliveness_report.face_bbox_ir.
height;
//返回数组
jintArray face_box_ir_array = env->NewIntArray(4);
env->
SetIntArrayRegion(face_box_ir_array,
0,4,face_box_ir_result);
env->
SetObjectField(jobj, jFace_bbox_ir, face_box_ir_array
);

Mat matRGB;



cvtColor(uliveness_report.rgb_face, matRGB, COLOR_BGR2BGRA);

//输出
jintArray result_rgb = env->NewIntArray(matRGB.rows * matRGB.cols);
jint *pResult_rgb = (jint *) matRGB.ptr(0);
env->SetIntArrayRegion(result_rgb,0, matRGB.rows *matRGB.cols, pResult_rgb);
env->SetIntField(jobj, jRGB_width, matRGB.cols);
env->SetIntField(jobj, jRGB_height, matRGB.rows);
env->SetObjectField(jobj, jRGBFace, result_rgb);




Mat matIR;

cvtColor(uliveness_report
.ir_speckle_face, matIR, COLOR_GRAY2BGRA);


//输出
jintArray result_ir = env->NewIntArray(matIR.rows * matIR.cols);
jint *pResult_ir = (jint *) matIR.ptr(0);
env->
SetIntArrayRegion(result_ir,
0, matIR.
rows *matIR
.cols, pResult_ir);
env->
SetIntField(jobj, jIR_width, matIR
.cols);
env->
SetIntField(jobj, jIR_height, matIR
.rows);
env->
SetObjectField(jobj, jIRFace, result_ir
);

}

env->
ReleaseByteArrayElements(imgObj, obj_native_data,
0);
env->
ReleaseByteArrayElements(imgRGB, rgb_native_data,
0);

return
jobj;


}


extern "C"
JNIEXPORT jobject
JNICALL
        Java_cc_uphoton_bctc_activity_MainActivity_FaceDetectLiveness(JNIEnv * env, jclass
type,
jintArray imgObj, jintArray
imgRGB,
jstring depth_data_path, jfloat
default_liveness
) {
//RGB和IR
jint *pRGB = env->GetIntArrayElements(imgRGB, NULL);
cv::Mat rgb_image(640, 480, CV_8UC4, (unsigned char *) pRGB);
cv::cvtColor(rgb_image, rgb_image, cv::COLOR_BGRA2BGR
);
//cv::imwrite("/storage/emulated/0/DetectDemo/test.jpg", rgb_image);

env->
ReleaseIntArrayElements(imgRGB, pRGB,
0);


jint *pObj = env->GetIntArrayElements(imgObj, NULL);
cv::Mat object_image(640, 400, CV_8UC4, (unsigned char *) pObj);
cv::cvtColor(object_image, object_image, CV_BGRA2BGR
);
//释放
env->
ReleaseIntArrayElements(imgObj, pObj,
0);


uphoton::uliveness::ULivenessReport uliveness_report;
// 设置计算深度图的参数
uphoton::udepthmap::CalculateSettings depth_settings;
uphoton::uliveness::FaceInputTypes face_input_source = uphoton::uliveness::IR_IMAGE_ONLY;

uint64_t start_time, end_time;
struct timeval tv;
gettimeofday(&tv,
nullptr);
start_time = static_cast<uint64_t>(tv.tv_sec) * 1000000 + tv.tv_usec;
//LOGD("lzh---:%d ", 1111);

uphoton::uliveness::GetLivenessByFaceDetect(rgb_image, object_image, uliveness_report, face_input_source);
//LOGD("lzh---:%d ", 2222);

gettimeofday(&tv,
nullptr);
end_time = static_cast<uint64_t>(tv.tv_sec) * 1000000 + tv.tv_usec;
double time = (end_time - start_time) / 1000.0;
//LOGD("lzh--score-:%f ms", time);
//LOGD("lzh--Face_net_time_only-:%f ms", uliveness_report.face_net_time_only);
//LOGD("lzh--face_detect_time-:%f ms", uliveness_report.face_detect_time);
//LOGD("lzh--live_net_time_only-:%f ms", uliveness_report.live_net_time_only);
//LOGD("lzh--live_detect_time-:%f ms", uliveness_report.live_detect_time);


jclass livenessResultBean = env->FindClass("cc/uphoton/bctc/bean/LivenessResultBean");
jfieldID jDepthFull = env->GetFieldID(livenessResultBean, "depth_full", "[I");
jfieldID jDepthFace = env->GetFieldID(livenessResultBean, "depth_face", "[I");
jfieldID jIRFace = env->GetFieldID(livenessResultBean, "ir_face", "[I");
jfieldID jRGBFace = env->GetFieldID(livenessResultBean, "rgb_face", "[I");
jfieldID jLiveness_probs = env->GetFieldID(livenessResultBean, "livness_probs", "[F");
jfieldID jFace_bbox_ir = env->GetFieldID(livenessResultBean, "face_bbox_ir", "[I");
jfieldID jDepth_width = env->GetFieldID(livenessResultBean, "depth_width", "I");
jfieldID jDepth_height = env->GetFieldID(livenessResultBean, "depth_height", "I");
jfieldID jIR_height = env->GetFieldID(livenessResultBean, "ir_height", "I");
jfieldID jIR_width = env->GetFieldID(livenessResultBean, "ir_width", "I");
jfieldID jRGB_width = env->GetFieldID(livenessResultBean, "rgb_width", "I");
jfieldID jRGB_height = env->GetFieldID(livenessResultBean, "rgb_height", "I");
jfieldID jHasFaceFlag = env->GetFieldID(livenessResultBean, "has_face_flag", "I");
jfieldID jCost_time = env->GetFieldID(livenessResultBean, "cost_time", "D");
jfieldID jFace_net_time_only = env->GetFieldID(livenessResultBean, "face_net_time_only", "F");
jfieldID jFace_detect_time_only = env->GetFieldID(livenessResultBean, "face_detect_time", "F");
jfieldID jLive_net_time_only = env->GetFieldID(livenessResultBean, "live_net_time_only", "F");
jfieldID jLive_detect_time = env->GetFieldID(livenessResultBean, "live_detect_time", "F");


//创建新的对象
jobject jobj = env->AllocObject(livenessResultBean);
env->
SetDoubleField(jobj, jCost_time, time
);


env->
SetIntField(jobj, jHasFaceFlag, uliveness_report
.has_face_flag);
if(uliveness_report.has_face_flag==1){
//返回数组
jfloatArray liveness_probs_array = env->NewFloatArray(2);
env->
SetFloatArrayRegion(liveness_probs_array,
0,2,uliveness_report.livness_probs);
env->
SetObjectField(jobj, jLiveness_probs, liveness_probs_array
);
int face_box_ir_result[4];
face_box_ir_result[0]=uliveness_report.face_bbox_ir.
x;
face_box_ir_result[1]=uliveness_report.face_bbox_ir.
y;
face_box_ir_result[2]=uliveness_report.face_bbox_ir.
width;
face_box_ir_result[3]=uliveness_report.face_bbox_ir.
height;
//返回数组
jintArray face_box_ir_array = env->NewIntArray(4);
env->
SetIntArrayRegion(face_box_ir_array,
0,4,face_box_ir_result);
env->
SetObjectField(jobj, jFace_bbox_ir, face_box_ir_array
);

Mat matRGB;
//LOGD("lzh---:%d ", 3333);

cvtColor(uliveness_report
.rgb_face, matRGB, COLOR_BGR2BGRA);
//LOGD("lzh---:%d ", 4444);

//输出
jintArray result_rgb = env->NewIntArray(matRGB.rows * matRGB.cols);
jint *pResult_rgb = (jint *) matRGB.ptr(0);
env->
SetIntArrayRegion(result_rgb,
0, matRGB.
rows *matRGB
.cols, pResult_rgb);

env->
SetIntField(jobj, jRGB_width, matRGB
.cols);
env->
SetIntField(jobj, jRGB_height, matRGB
.rows);
env->
SetObjectField(jobj, jRGBFace, result_rgb
);


Mat matIR;
cvtColor(uliveness_report
.ir_speckle_face, matIR, COLOR_GRAY2BGRA);

//输出
jintArray result_ir = env->NewIntArray(matIR.rows * matIR.cols);
jint *pResult_ir = (jint *) matIR.ptr(0);
env->
SetIntArrayRegion(result_ir,
0, matIR.
rows *matIR
.cols, pResult_ir);
env->
SetIntField(jobj, jIR_width, matIR
.cols);
env->
SetIntField(jobj, jIR_height, matIR
.rows);
env->
SetObjectField(jobj, jIRFace, result_ir
);
/*if(uliveness_report.livness_probs[1]>default_liveness ){

const char *native_depth_data_path = env->GetStringUTFChars(depth_data_path, 0);
uphoton::uliveness::SaveDepthMapToBinaryFile(uliveness_report
.depthmap_vga,native_depth_data_path);
env->
ReleaseStringUTFChars(depth_data_path, native_depth_data_path
);
//深度图
Mat matDepthVGA;
cvtColor(uliveness_report
.depthmap_pcolor_vga, matDepthVGA, COLOR_BGR2BGRA);
//cv::imwrite("/storage/emulated/0/LivenessResult/depth.jpg",matDepthVGA);

jintArray result_full = env->NewIntArray(matDepthVGA.rows * matDepthVGA.cols);
jint *pResult = (jint *)
        matDepthVGA.ptr(0);
env->
SetIntArrayRegion(result_full,
0, matDepthVGA.
rows *matDepthVGA
.cols, pResult);
env->
SetObjectField(jobj, jDepthFull, result_full);
}*/
}



return
jobj;


}





/*extern "C"
JNIEXPORT jobject
JNICALL
        Java_cc_uphoton_bctc_activity_MainActivity_CalLiveness(JNIEnv * env, jclass
type,
jintArray imgObj, jintArray
imgRGB,
jobjectArray jobject_Array, jint
depth_mode,
jint vertical_range, jint
is_rgb,
jint is_new_materials
) {

//RGB和IR
jint *pRGB = env->GetIntArrayElements(imgRGB, NULL);
cv::Mat rgb_image(640, 480, CV_8UC4, (unsigned char *) pRGB);
cv::cvtColor(rgb_image, rgb_image, cv::COLOR_BGR2BGRA
);
rgb_image.
convertTo(rgb_image, CV_8UC3
);
//释放
env->
ReleaseIntArrayElements(imgRGB, pRGB,
0);
jint *pObj = env->GetIntArrayElements(imgObj, NULL);
cv::Mat object_image(1280, 1080, CV_8UC4, (unsigned char *) pObj);
//LOGD("lzh--imgObj.length-:%d", imgObj);
cv::cvtColor(object_image, object_image, CV_BGRA2GRAY
);

object_image.
convertTo(object_image, CV_8UC1
);
//释放
env->
ReleaseIntArrayElements(imgObj, pObj,
0);

jclass reportBean = env->FindClass("cc/uphoton/bctc/bean/FaceDetectionReportBean");
jfieldID face_id_class = env->GetFieldID(reportBean, "face_id", "I");
jfieldID key_points_size_class = env->GetFieldID(reportBean, "key_points_size", "I");
jfieldID score_class = env->GetFieldID(reportBean, "score", "F");
jfieldID yaw_class = env->GetFieldID(reportBean, "yaw", "F");
jfieldID pitch_class = env->GetFieldID(reportBean, "pitch", "F");
jfieldID roll_class = env->GetFieldID(reportBean, "roll", "F");
jfieldID face_action_class = env->GetFieldID(reportBean, "face_action", "J");
jfieldID face_bbox_class = env->GetFieldID(reportBean, "face_bbox", "[I");
jfieldID key_points_class = env->GetFieldID(reportBean, "key_points", "[[F");
jfieldID visibilities_class = env->GetFieldID(reportBean, "visibilities", "[F");
jint objLength = env->GetArrayLength(jobject_Array);

uphoton::uface::FaceDetectionReport face_report[10];
for(
int k = 0;
k<objLength;
k++){
jobject obj_ele = (jobject)
env->
GetObjectArrayElement(jobject_Array, k
);
jint face_id = env->GetIntField(obj_ele, face_id_class);
jint key_points_size = env->GetIntField(obj_ele, key_points_size_class);
jfloat score = env->GetFloatField(obj_ele, score_class);
jfloat yaw = env->GetFloatField(obj_ele, yaw_class);
jfloat pitch = env->GetFloatField(obj_ele, pitch_class);
jfloat roll = env->GetFloatField(obj_ele, roll_class);
jlong face_action = env->GetLongField(obj_ele, face_action_class);
jfloatArray visibilities = (jfloatArray) env->GetObjectField(obj_ele, visibilities_class);
jint visibilities_len = env->GetArrayLength(visibilities);
jfloat *visibilities_ele = env->GetFloatArrayElements(visibilities, 0);

for (
int i = 0;
i<visibilities_len;
i++) {
face_report[k].visibilities[i] = visibilities_ele[i];
}
//释放
env->
ReleaseFloatArrayElements(visibilities, visibilities_ele,
0);

jintArray face_bbox = (jintArray)
env->
GetObjectField(obj_ele, face_bbox_class
);
jint face_bbox_len = env->GetArrayLength(face_bbox);
jint *face_bbox_ele = env->GetIntArrayElements(face_bbox, 0);

for (
int i = 0;
i<face_bbox_len;
i++) {
face_report[k].face_bbox[i] = face_bbox_ele[i];
}
//释放
env->
ReleaseIntArrayElements(face_bbox, face_bbox_ele,
0);

jobjectArray key_points = (jobjectArray)
        env->
                GetObjectField(obj_ele, key_points_class
        );
jint key_points_row = env->GetArrayLength(key_points);
for (
int m = 0;
m<key_points_row;
m++) {
jfloatArray array = (jfloatArray) env->GetObjectArrayElement(key_points, m);
jint key_points_cols = env->GetArrayLength(array);
jfloat *coldata = env->GetFloatArrayElements((jfloatArray) array, nullptr);
for (
int n = 0;
n<key_points_cols;
n++) {
face_report[k].key_points[m][n] = coldata[n];
}
//释放
env->
ReleaseFloatArrayElements(array, coldata,
0);

}
face_report[k].
face_id = face_id;
face_report[k].
key_points_size = key_points_size;
face_report[k].
score = score;
face_report[k].
yaw = yaw;
face_report[k].
pitch = pitch;
face_report[k].
roll = roll;
face_report[k].
face_action = face_action;
}


// 设置计算深度图的参数
uphoton::udepthmap::CalculateSettings depth_settings;
// 设置是否使用后处理
depth_settings.
post_process = true;
// 如果使用后处理，设置是否进行孔洞填充
depth_settings.
hole_fill_flag = false;
// 如果使用孔洞填充，设置孔洞填充窗口大小
depth_settings.
hole_win_radius = uphoton::udepthmap::HOLE_WINRADIUS_3;
// 设置是否进行深度预估
depth_settings.
depth_estimate = false;
// 如果使用深度预估，设置深度预估范围
depth_settings.
depth_estimate_forward = -3;  // cm
depth_settings.
depth_estimate_backward = 3;  // cm
// 设置ROI区域
depth_settings.
roi_bbox = cv::Rect(0, 0, 480, 640);
// 设置输出分辨率
depth_settings.
output_sample = uphoton::udepthmap::SAMPLE_NUM_ROI;
// 设置匹配时垂直方向搜索范围
if(vertical_range==0){
depth_settings.
vertical_search_range = uphoton::udepthmap::VERTICAL_RANGE_0;
}else if(vertical_range==1){
depth_settings.
vertical_search_range = uphoton::udepthmap::VERTICAL_RANGE_1;

}

// 设置是否使用置信度滤波
depth_settings.
filter_type = uphoton::udepthmap::FILTER_MEAN;
depth_settings.
cost_filter_size = uphoton::udepthmap::COST_WINSIZE_5;

// 目前sdk提供两种计算置信度的方式：MEAN_CCORR_NORMED和STD_CCORR_NORMED
// 注意：选择不同计算置信度方式match_win_size参数值不同
// 使用MEAN_CCORR_NORMED方式计算置信度
depth_settings.
cost_cal_type = uphoton::udepthmap::MEAN_CCORR_NORMED;
// 设置匹配窗口大小
depth_settings.
match_win_size = uphoton::udepthmap::MATCH_WINSIZE_13;

*//*******************************************
// 如果选择STD_CCORR_NORMED时
// 使用STD_CCORR_NORMED方式计算置信度
settings.cost_cal_type = udepthmap::STD_CCORR_NORMED;
// 设置匹配窗口大小
settings.match_win_size = udepthmap::MATCH_WINSIZE_17;
********************************************//*

cv::Mat ir_face_ellipse;
cv::Mat depth_face_ellipse;
cv::Mat depth_face_gray_image;
//uphoton::uliveness::GetLivenessByFaceDetect(rgb_image, object_image, depth_settings);
const std::size_t &face_num = objLength;
uphoton::uliveness::ULivenessReport uliveness_report;
bool depth_mode_flag;
if(depth_mode==1){
depth_mode_flag = true;
}else{
depth_mode_flag = false;
}
bool isRGB;
if(is_rgb==1){
isRGB = true;
}else{
isRGB = false;
}
bool isNewMaterials;
if(is_new_materials==1){
isNewMaterials = true;
}else{
isNewMaterials = false;

}

uint64_t start_time, end_time;
struct timeval tv;
gettimeofday(&tv,
nullptr);
start_time = static_cast<uint64_t>(tv.tv_sec) * 1000000 + tv.tv_usec;
uphoton::uliveness::GetLivenessByFaceReport(rgb_image, object_image, uliveness_report, face_num, isRGB, face_report, depth_mode_flag, depth_settings
);
gettimeofday(&tv,
nullptr);
end_time = static_cast<uint64_t>(tv.tv_sec) * 1000000 + tv.tv_usec;
double time = (end_time - start_time) / 1000.0;
//LOGD("lzh--活体算法耗时-:%f ms", time);


jclass livenessResultBean = env->FindClass("cc/uphoton/bctc/bean/LivenessResultBean");
jfieldID jDepthFull = env->GetFieldID(livenessResultBean, "depth_full", "[I");
jfieldID jDepthFace = env->GetFieldID(livenessResultBean, "depth_face", "[I");
jfieldID jIRFace = env->GetFieldID(livenessResultBean, "ir_face", "[I");
jfieldID jRGBFace = env->GetFieldID(livenessResultBean, "rgb_face", "[I");
jfieldID jLiveness_probs = env->GetFieldID(livenessResultBean, "livness_probs", "[F");
jfieldID jDepth_width = env->GetFieldID(livenessResultBean, "depth_width", "I");
jfieldID jDepth_height = env->GetFieldID(livenessResultBean, "depth_height", "I");
jfieldID jIR_height = env->GetFieldID(livenessResultBean, "ir_height", "I");
jfieldID jIR_width = env->GetFieldID(livenessResultBean, "ir_width", "I");
jfieldID jRGB_width = env->GetFieldID(livenessResultBean, "rgb_width", "I");
jfieldID jRGB_height = env->GetFieldID(livenessResultBean, "rgb_height", "I");
jfieldID jCost_time = env->GetFieldID(livenessResultBean, "cost_time", "D");

//创建新的对象
jobject jobj = env->AllocObject(livenessResultBean);
//env->SetDoubleField(jobj, jCost_time, endtime * 1000);
env->
SetDoubleField(jobj, jCost_time, time
);
//LOGD("lzh--C耗时-:%f ms", endtime*1000);
//返回数组
jfloatArray liveness_probs_array = env->NewFloatArray(2);
env->
SetFloatArrayRegion(liveness_probs_array,
0,2,uliveness_report.livness_probs);
env->
SetObjectField(jobj, jLiveness_probs, liveness_probs_array
);
//释放
//env->ReleaseFloatArrayElements(visibilities, visibilities_ele,0);

Mat matRGB;
cvtColor(uliveness_report
.rgb_face, matRGB, COLOR_BGR2BGRA);
//输出
jintArray result_rgb = env->NewIntArray(matRGB.rows * matRGB.cols);
jint *pResult_rgb = (jint * )
matRGB.ptr(0);
env->
SetIntArrayRegion(result_rgb,
0, matRGB.
rows *matRGB
.cols, pResult_rgb);

env->
SetIntField(jobj, jRGB_width, matRGB
.cols);
env->
SetIntField(jobj, jRGB_height, matRGB
.rows);
env->
SetObjectField(jobj, jRGBFace, result_rgb
);


Mat matIR;
cvtColor(uliveness_report
.ir_speckle_face, matIR, COLOR_GRAY2BGRA);

//输出
jintArray result_ir = env->NewIntArray(matIR.rows * matIR.cols);
jint *pResult_ir = (jint * )
matIR.ptr(0);
env->
SetIntArrayRegion(result_ir,
0, matIR.
rows *matIR
.cols, pResult_ir);
env->
SetIntField(jobj, jIR_width, matIR
.cols);
env->
SetIntField(jobj, jIR_height, matIR
.rows);
env->
SetObjectField(jobj, jIRFace, result_ir
);

*//*env->DeleteLocalRef(result_ir);
free(pResult_ir);*//*

if(depth_mode_flag){
Mat matDepthFull, matDepthFace;
Mat matFull, matFace;

uphoton::uliveness::GetDepthPseudoColorMap(uliveness_report
.depthmap_full,
matFull,
uphoton::udepthmap::COLOR_TURBO,
0, 255);
uphoton::uliveness::GetDepthPseudoColorMap(uliveness_report
.depthmap_face,
matFace,
uphoton::udepthmap::COLOR_TURBO,0, 255);

cvtColor(matFace, matDepthFace, COLOR_BGR2BGRA
);

cvtColor(matFull, matDepthFull, COLOR_BGR2BGRA
);



//cv::imwrite("/storage/emulated/0/LivenessResult/depth_full.jpg", matDepthFull);
//cv::imwrite("/storage/emulated/0/LivenessResult/depth_face.jpg", matDepthFace);

env->
SetIntField(jobj, jDepth_width, matDepthFace
.cols);
env->
SetIntField(jobj, jDepth_height, matDepthFace
.rows);

jintArray result_full = env->NewIntArray(matDepthFull.rows * matDepthFull.cols);
jint *pResult = (jint * )
matDepthFull.ptr(0);
env->
SetIntArrayRegion(result_full,
0, matDepthFull.
rows *matDepthFull
.cols, pResult);
env->
SetObjectField(jobj, jDepthFull, result_full
);

jintArray result_face = env->NewIntArray(matDepthFace.rows * matDepthFace.cols);
jint *pResult_face = (jint * )
matDepthFace.ptr(0);
env->
SetIntArrayRegion(result_face,
0, matDepthFace.
rows *matDepthFace
.cols, pResult_face);
env->
SetObjectField(jobj, jDepthFace, result_face
);

}


return
jobj;
}*/
extern "C"
JNIEXPORT jintArray
JNICALL
        Java_cc_uphoton_bctc_activity_MainActivity_AdjustLightness(JNIEnv * env, jclass
type,
jintArray imgObj
){
jint *pObj = env->GetIntArrayElements(imgObj, NULL);
cv::Mat object_image(1280, 1080, CV_8UC4, (unsigned char *) pObj);
cv::cvtColor(object_image, object_image, CV_BGRA2GRAY
);

object_image.
convertTo(object_image, CV_8UC1
);
env->
ReleaseIntArrayElements(imgObj, pObj,
0);
// 测试AdjustLightness函数
cv::Mat object_gamma_image;
//新版本没有此功能注释掉此句
//uphoton::uliveness::AdjustLightness(object_image, object_gamma_image);

//cv::imwrite("/storage/emulated/0/CalibrationTool/no_gamma.bmp", object_image);
//cv::imwrite("/storage/emulated/0/CalibrationTool/after_gamma.bmp", object_gamma_image);
cv::cvtColor(object_gamma_image, object_gamma_image, COLOR_GRAY2BGRA
);

jintArray result = env->NewIntArray(object_gamma_image.rows * object_gamma_image.cols);
jint *pResult = (jint *)
        object_gamma_image.ptr(0);
env->
SetIntArrayRegion(result,
0, object_gamma_image.
rows *object_gamma_image
.cols, pResult);
return
result;

}
