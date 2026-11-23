# ML Kit ships consumer rules. Keep Atlas model classes readable in crash traces.
-keepattributes SourceFile,LineNumberTable

# 0.5 — native libraries call back into these classes through JNI; R8 must not rename or strip them.
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class net.zetetic.database.** { *; }
-dontwarn com.google.ai.edge.litertlm.**
