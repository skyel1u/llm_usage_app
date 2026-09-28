# BUILD

```
./gradlew.bat assembleRelease --console=plain
```

## Release 签名

本地与 CI 共用同一把 keystore,保证覆盖安装不报签名冲突:

- **本地**:仓库根放 `release.keystore` + `keystore.properties`(均已在 .gitignore):
  ```
  storeFile=release.keystore
  storePassword=<密码>
  keyAlias=llmusage
  keyPassword=<密码>
  ```
- **CI**:仓库 Secrets 配置 `KEYSTORE_BASE64`(keystore 文件的 base64)、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`,workflow 自动解码注入。
- 两者都缺失时回退 debug 签名(仅本机自用,勿分发)。

生成新 keystore(仅首次):
```
keytool -genkeypair -v -keystore release.keystore -alias llmusage -keyalg RSA -keysize 2048 -validity 10950
```

> ⚠️ **keystore 或密码丢失 = 无法覆盖安装已有应用**(只能卸载重装,丢失应用内全部账户与历史)。
> 请把 `release.keystore` 与密码一起备份到安全位置(密码管理器 / 加密网盘)。
