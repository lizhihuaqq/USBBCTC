#ifndef UFACE_TYPES
#define UFACE_TYPES

#include <string>
#include <opencv2/opencv.hpp>

namespace uphoton {
namespace uface {

#define MAX_FACE_NUM        10        ///< 图像中人脸个数最多为10个

enum FaceDetectorTypes {
  MNNKIT_FACE_TYPE        = 0,        ///< MNNKit人脸检测器
  DLIB_FACE_TYPE          = 1,        ///< Dlib人脸检测器
  UPHOTON_FACE_TYPE       = 2,        ///< 驭光人脸检测器
};

enum FaceResultTypes {
  GET_MAX_FACE            = 0,        ///< 返回检测到的最大面积人脸
  GET_ALL_FACE            = 1,        ///< 返回检测到的所有人脸
};

/**@enum FaceActionTypes
* @brief 脸部动作枚举类型 \n
* 使用：通过EYE_BLINK&face_action判断是否眨眼  \n
*/
enum FaceActionTypes {
  EYE_BLINK               = 1<<1,     ///< 眨眼
  MOUTH_AH                = 1<<2,     ///< 嘴巴大张
  HEAD_YAW                = 1<<3,     ///< 摇头
  HEAD_PITCH              = 1<<4,     ///< 点头
  BROW_JUMP               = 1<<5,     ///< 眉毛挑动
};

/**@struct FaceDetectionReport
* @brief 人脸检测结果结构体 \n
* 此结构体可保存Dlib和MNNKit两种检测器的结果 \n
* 注意:face_bbox和key_points点的位置必须在480x640分辨率下
*/
struct FaceDetectionReport {
  int face_bbox[4];         ///< 人脸矩形框左上角,[0]:左上角x,[1]:左上角y,[2]:宽度,[3]:高度
  int face_id;              ///< 每个检测到的人脸拥有唯一的face_id
  int key_points_size;      ///< 关键点个数。MNNKit有106个，Dlib有68个
  float key_points[106][2]; ///< 人脸关键点的数组，因为不会多于106个，所以数组大小固定
  float visibilities[106];  ///< 对应点的能见度,点未被遮挡1.0,被遮挡0.0
  float score;              ///< 置信度
  float yaw;                ///< 水平转角,真实度量的左负右正
  float pitch;              ///< 俯仰角,真实度量的上负下正
  float roll;               ///< 旋转角,真实度量的左负右正
  long face_action;         ///< 脸部动作,暂时保留，一般在视频检测模式中使用，因为动作检测需要连续几帧的信息
};

}  // namespace uface
}  // namespace uphoton

#endif // UFACE_TYPES
