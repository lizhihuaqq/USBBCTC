#ifndef ULIVENESS_H
#define ULIVENESS_H

#include "uliveness_types.h"
#include <opencv2/opencv.hpp>

#define ULIVENESS_VERSION          "0.3.1"

namespace uphoton {
namespace uliveness {

void Init(const UFaceMaskConfig &face_mask_config,
          const ULiveNetConfig &live_net_config);

bool GetLivenessByFaceDetect(
    const cv::Mat &rgb_image,
    const cv::Mat &ir_image,
    ULivenessReport &report,
    const FaceInputTypes &face_input_source);

bool GetLivenessByFaceReport(
    const cv::Mat &rgb_image,
    const cv::Mat &ir_image,
    ULivenessReport &report,
    const std::size_t &face_num,
    const bool &rgb_detect_flag,
    const uface::FaceDetectionReport face_report[],
    const bool &depth_mode_flag);

// AdjustLightness: 只在经过亮度调节的NIR图上检测人脸，
// 只有当gamma值小于等于max_gamma阈值时才进行亮度调节
// 人脸检测成功后，活体检测使用未经亮度调节的原图
// image_in尺寸为1080x1280
// image_out尺寸为480x640
void AdjustLightness(const cv::Mat &image_in,
                     cv::Mat &image_out,
                     const double &max_gamma = 255);

void GetDepthPseudoColorMap(const cv::Mat &depth_map,
                            cv::Mat &color_map,
                            const udepthmap::PseudoColorTypes &types,
                            const uchar &min,
                            const uchar &max);

bool SaveDepthMapToBinaryFile(const cv::Mat &image,
                              const char* file);
}  // namespace uliveness
}  // namespace uphoton

#endif // ULIVENESS_H
