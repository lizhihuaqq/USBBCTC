package cc.uphoton.bctc.bean;

import java.io.Serializable;

public class LivenessResultBean implements Serializable {
    public LivenessResultBean() {
    }

    //深度图数据
    public int[] depth_full;
    public int[] depth_face;
    public int[] ir_face;
    public int[] rgb_face;
    public float livness_probs[] = new float[2];
    public int face_bbox_ir[] = new int[4];
    public int depth_width;
    public int depth_height;
    public int ir_height;
    public int ir_width;
    public int rgb_width;
    public int rgb_height;
    public double cost_time;
    public int has_face_flag;
    public float face_net_time_only;     ///< 运行人脸检测模型耗时(ms),不包括预处理和其他业务处理时间
    public float face_detect_time;       ///< 人脸检测全流程耗时(ms)
    public float live_net_time_only;     ///< 运行活体检测模型耗时(ms),不包括预处理和其他业务处理时间
    public float live_detect_time;       ///< 活体检测全流程耗时(ms)
}
