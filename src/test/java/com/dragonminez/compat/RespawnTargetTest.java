package com.dragonminez.compat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.AnnotationNode;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Keep the respawn hook aligned with the exact configured NeoForge PlayerList bytecode. */
class RespawnTargetTest {
    private static ClassNode read(String name) throws Exception {
        var node = new ClassNode();
        try (var input = RespawnTargetTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name); new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG);
        }
        return node;
    }
    @Test void respawnSelectorsExistInPinnedNeoForgeAndUseActualHealthReceiver() throws Exception {
        var target = read("net/minecraft/server/players/PlayerList");
        var mixin = read("com/dragonminez/mixin/common/PlayerListMixin");
        int checked = 0;
        for (var method : mixin.methods) {
            var annotations = new ArrayList<AnnotationNode>();
            if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
            if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
            for (AnnotationNode annotation : annotations) {
                for (int i = 0; i < annotation.values.size(); i += 2) {
                    if (!annotation.values.get(i).equals("method")) continue;
                    for (Object selector : (List<?>) annotation.values.get(i + 1)) {
                        String selected = selector.toString(); checked++;
                        assertTrue(target.methods.stream().anyMatch(m -> selected.equals(m.name + m.desc)),
                                "Missing pinned NeoForge respawn target: " + selected);
                    }
                }
            }
        }
        assertEquals(1, checked);
        var respawn = target.methods.stream().filter(m -> m.name.equals("respawn")).findFirst().orElseThrow();
        assertTrue(java.util.stream.StreamSupport.stream(respawn.instructions.spliterator(), false)
                .anyMatch(n -> n instanceof org.objectweb.asm.tree.MethodInsnNode call
                        && call.owner.equals("net/minecraft/server/level/ServerPlayer")
                        && call.name.equals("setHealth") && call.desc.equals("(F)V")));
    }
}
