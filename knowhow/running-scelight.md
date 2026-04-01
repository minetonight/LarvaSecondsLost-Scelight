Scelight is expecting Java 8. The package name javax.xml.bind.* is the key clue: old apps commonly depended on that being included by default, while modern OpenJDK releases do not include it unless you add compatibility jars manually.

```bash
/usr/lib/jvm/java-8-openjdk-amd64/bin/java -Xmx1024m -Dfile.encoding=UTF-8 -Dhu.scelight.launched-with=Scelight-linux.sh -cp mod/launcher/3.1.4/scelight-launcher.sldat:mod/launcher/3.1.4/jl1.0.1.jar:mod/launcher/3.1.4/mp3spi1.9.5.jar:mod/launcher/3.1.4/tritonus_share.jar hu/sllauncher/ScelightLauncher
```