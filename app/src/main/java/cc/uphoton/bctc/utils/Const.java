package cc.uphoton.bctc.utils;

public class Const {
    public enum CaptureType {
        DEPTH, IR, RGB
    }
    public static String COLLECT_IR_FINISH_ACTION_ONCE = "collect.ir.finish.action.once";
    public static String COLLECT_RGB_FINISH_ACTION_ONCE = "collect.rgb.finish.action.once";
    public static String COLLECT_DEPTH_FINISH_ACTION_ONCE = "collect.depth.finish.action.once";
    public static String COLLECT_IR_FINISH_ACTION = "collect.ir.finish.action";
    public static String COLLECT_Depth_FINISH_ACTION = "collect.depth.finish.action";
    public static String COLLECT_RGB_FINISH_ACTION = "collect.rgb.finish.action";
    public static String PREVIEW = "preview";
    public static String IR_PREVIEW = "ir";
    public static String Depth_PREVIEW = "depth";
    public static String TAKE_PIC_IR = "TAKE_PIC_IR";
    public static String TAKE_PIC_DEPTH = "TAKE_PIC_DEPTH";
    public static String TAKE_PIC_RGB = "TAKE_PIC_RGB";
    public static String IS_ONLY_TAKE_PIC = "is_only_take_pic";
    public static String IS_ONLY_TAKE_PIC_IR = "is_only_take_pic_ir";
    public static String IS_ONLY_TAKE_PIC_RGB= "is_only_take_pic_rgb";
    public static String IS_USE_RESOURCE = "is_use_resource";
    public static String TOTAL_TIME = "total_time";
    public static String SERVER_IS_RUNNING = "server is running";
    public static String THRESHOLD_VALUE_DEFAULT = "0.75";
    public static String EXPOSURE_VALUE_DEFAULT = "4";
    public static String THRESHOLD_VALUE = "threshold_value";
    public static String SP_NAME = "sp_facedetect";
    public static String APP_IS_First_RUN = "APP_IS_First_RUN";
    public static String JGG_BTN_IS_CHECKED = "JGG_BTN_IS_CHECKED";
    public static String FZ_DIANL = "fengzhi";
    public static String ZHAN_KONG_BI = "zhan_kong_bi";
    public static String JGG_FZ = "jgg_fengzhi";
    public static String JGG_DL = "jgg_dianliu";
    public static String FG_FZ = "fg_fengzhi";
    public static String FG_DL = "fg_dianliu";
    public static String SETTING_DEPTH_WEI = "setting_depth_wei";
    public static String REF_PATH = "ref_path";

    public static String YML_PATH = "/storage/emulated/0/CalibrationTool/Remap_RGB2IR.yml";
    public static String YML_PATH_NEW = "/storage/emulated/0/CalibrationTool/rgb_to_ir_remap_matrix.yml";
    public static String REF_DEFAULT_PATH = "/storage/emulated/0/CalibrationTool/Ref.jpg";
    public static String LIVE_MODEL_PATH = "/storage/emulated/0/CalibrationTool/ulivenet_ir_v0.10.0_thresh55.mnn";
    public static String FACE_MODEL_PATH = "/storage/emulated/0/CalibrationTool/ultra_ssd_rfb_epoch75.mnn";

    //算法版本V0.4.0，APP 版本1.4
    public static String IsAutoRun = "false";
    public static String Interface_Value = "Interface_Value";
    public static String ISO_VALUE = "ISO_VALUE";
    public static String EXPOSURE_VALUE = "EXPOSURE_VALUE";
    public static String ISO_AUTO = "auto";
    public static String ISO_100 = "100";
    public static String ISO_200 = "200";
    public static String ISO_400 = "400";
    public static String ISO_800 = "800";
    public static String ISO_1600 = "1600";
    public static String RANGE_VALUE_ROI = "RANGE_VALUE_ROI";
    public static String RANGE_VALUE_VGA = "RANGE_VALUE_VGA";
    public static String RGB = "_RGB.jpg";
    public static String DES_RGB = "_RGB.jpg";
    public static String RGB_ROI = "_RGB_ROI.jpg";
    public static String IR_SPECKLE = "_IR_speckle.jpg";
    public static String DES_IR_SPECKLE = "_IR_speckle.jpg";
    public static String IR_SPECKLE_ROI = "_IR_speckle_ROI.jpg";
    public static String IR_FLOOD = "_IR_flood.jpg";
    public static String IR_FLOOD_ROI = "_IR_flood_ROI.jpg";
    public static String DEPTH_PIC = "_Depth.jpg";
    public static String DEPTH_DATA = "_Depth.dat";
    public static String CLICK_ONCE = "click once";
    public static String SAVE_TRUE = "save_true";
    public static String SAVE_FALSE = "save_false";
    public static String SAVE_ALL = "save_all";
    public static String POINTX = "pointX";
    public static String POINTY = "pointY";
    public static String WIDTH = "width";
    public static String HEIGHT = "Height";
    public static String TIME_STAMP = "time_stamp";
    public static String TIME_Default = "localtime";
    public static String START_CHECK_ORDER = "start check liveness.the time is";
    public static String SAVE_FILE_DEFAULT = "LivenessResult";
    public static String DEPTH_MODEL = "Depth_model";
    public static String VERTICAL_RANGE = "vertical_range";
    public static String IS_RGB_DETEDTED_FACE = "is_rgb_detected_face";
    public static String SUM_COST_TIME = "sum_cost_time";
    public static String LIVE_COST_TIME = "live_cost_time";
    public static String FACE_COST_TIME = "face_cost_time";
    public static String PIC_COST_TIME = "pic_cost_time";


}
