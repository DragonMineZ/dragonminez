package com.dragonminez.core.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public class TextUtil {

  /**
   * Recursively pretty-prints the NBT tag content into a human-readable string. Uses indentation to
   * represent nested compound and list tags.
   *
   * @param tag the NBT tag to serialize as string
   * @return a pretty-printed multiline string representation of the tag
   */
  public static String prettyPrintNBT(Tag tag) {
    StringBuilder sb = new StringBuilder();
    tag.accept(new StringTagVisitor() {
      private int indent = 0;

      private void appendIndent() {
        sb.append("  ".repeat(indent));
      }

      @Override
      public void visitString(@NotNull StringTag stringTag) {
        appendIndent();
        sb.append(stringTag).append("\n");
      }

      @Override
      public void visitList(@NotNull ListTag listTag) {
        appendIndent();
        sb.append("List[").append(listTag.size()).append("]:\n");
        indent++;
        for (Tag element : listTag) {
          element.accept(this);
        }
        indent--;
      }

      @Override
      public void visitCompound(@NotNull CompoundTag compoundTag) {
        appendIndent();
        indent++;
        sb.append("\n");
        for (String key : compoundTag.getAllKeys()) {
          Tag value = compoundTag.get(key);
          appendIndent();
          sb.append(key).append(": ");
          if (value instanceof CompoundTag || value instanceof ListTag) {
            sb.append("\n");
            value.accept(this);
          } else {
            sb.append(value).append("\n");
          }
        }
        indent--;
        appendIndent();
        sb.append("\n");
      }

      @Override
      public void visitEnd(@NotNull EndTag endTag) {
        appendIndent();
        sb.append("END\n");
      }
    });
    return sb.toString();
  }

}
