// automatically generated by the FlatBuffers compiler, do not modify

package jp.oist.abcvlib.core.learning.fbclasses;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class MotionAction extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_1_12_0(); }
  public static MotionAction getRootAsMotionAction(ByteBuffer _bb) { return getRootAsMotionAction(_bb, new MotionAction()); }
  public static MotionAction getRootAsMotionAction(ByteBuffer _bb, MotionAction obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public MotionAction __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public byte actionByte() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }
  public String actionName() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer actionNameAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }
  public ByteBuffer actionNameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 6, 1); }
  public int leftWheel() { int o = __offset(8); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public int rightWheel() { int o = __offset(10); return o != 0 ? bb.getInt(o + bb_pos) : 0; }

  public static int createMotionAction(FlatBufferBuilder builder,
      byte action_byte,
      int action_nameOffset,
      int left_wheel,
      int right_wheel) {
    builder.startTable(4);
    MotionAction.addRightWheel(builder, right_wheel);
    MotionAction.addLeftWheel(builder, left_wheel);
    MotionAction.addActionName(builder, action_nameOffset);
    MotionAction.addActionByte(builder, action_byte);
    return MotionAction.endMotionAction(builder);
  }

  public static void startMotionAction(FlatBufferBuilder builder) { builder.startTable(4); }
  public static void addActionByte(FlatBufferBuilder builder, byte actionByte) { builder.addByte(0, actionByte, 0); }
  public static void addActionName(FlatBufferBuilder builder, int actionNameOffset) { builder.addOffset(1, actionNameOffset, 0); }
  public static void addLeftWheel(FlatBufferBuilder builder, int leftWheel) { builder.addInt(2, leftWheel, 0); }
  public static void addRightWheel(FlatBufferBuilder builder, int rightWheel) { builder.addInt(3, rightWheel, 0); }
  public static int endMotionAction(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public MotionAction get(int j) { return get(new MotionAction(), j); }
    public MotionAction get(MotionAction obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

