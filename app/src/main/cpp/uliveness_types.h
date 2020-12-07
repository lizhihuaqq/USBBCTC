#ifndef ULIVENESS_TYPES
#define ULIVENESS_TYPES

#include "udepthmap_types.h"
#include "uface_types.h"
#include "ucalibrate_types.h"
#include <string>
#include <opencv2/opencv.hpp>

namespace uphoton {
namespace uliveness {

enum FaceInputTypes {
  IR_IMAGE_ONLY           = 0,        ///< 只使用IR进行人脸检测
  RGB_IR_IMAGE            = 1,        ///< 首先在RGB图上进行检测，检测不到再在IR上检测
};

enum PowerModeTypes {
  POWER_NORMAL        = 0,
  POWER_HIGH          = 1,
  POWER_LOW           = 2
};

enum PrecisionModeTypes {
  PRECISION_NORMAL    = 0,
  PRECISION_HIGH      = 1,
  PRECISION_LOW       = 2,
};

enum MemoryModeTypes {
  MEMORY_NORMAL       = 0,
  MEMORY_HIGH         = 1,
  MEMORY_LOW          = 2,
};

struct ULiveNetConfig {
  int number_thread;
  MemoryModeTypes memory;
  PrecisionModeTypes precision;
  PowerModeTypes power;
};

struct UFaceMaskConfig {
  udepthmap::InitParams depth_init_params;
  cv::Mat P;
  ucalibrate::CameraDirectionTypes direction;
  uface::FaceDetectorTypes face_type;
  const char *reference_image_path;
};

struct ULivenessReport {
  bool has_face_flag;         ///< 是否存在人脸
  float livness_probs[2];     ///< 假体活体二分类概率，livness_probs[0]:假体，livness_probs[1]:活体
  cv::Rect face_bbox_ir;      ///< 在ir图上的人脸区域。注意：face_bbox_ir是在960x1280上取的人脸框
  cv::Mat ir_speckle_face;    ///< 用于活体检测的人脸散斑图，uchar
  cv::Mat rgb_face;           ///< 和ir_speckle_face对应的人脸rgb图
};

}  // namespace uliveness
}  // namespace uphoton

#endif // ULIVENESS_TYPES
