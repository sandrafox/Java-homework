cd java
"C:\Program Files\Java\jdk1.8.0_151\bin\javac.exe" -cp ../artifacts/ImplementorTest.jar ru/ifmo/rain/lisicyna/implementor/*.java

SET pack_kg=info/kgeorgiy/java/advanced/implementor/


"C:\Program Files\Java\jdk1.8.0_151\bin\jar.exe" xf ../artifacts/ImplementorTest.jar %pack_kg%Impler.class %pack_kg%JarImpler.class %pack_kg%ImplerException.class
"C:\Program Files\Java\jdk1.8.0_151\bin\jar.exe" cfm Implementor.jar ..\MANIFEST.mf ru/ifmo/rain/lisicyna/implementor/*.class %pack_kg%*.class

"C:\Program Files\Java\jdk1.8.0_151\bin\java.exe" -jar Implementor.jar -jar java.util.Set out.jar