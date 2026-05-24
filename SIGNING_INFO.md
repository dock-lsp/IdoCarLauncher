# IdoCarLauncher 签名信息

> ⚠️ **安全警告**：此文件包含敏感签名信息，请妥善保管，不要公开分享！

## 密钥库文件

### 文件信息
| 属性 | 值 |
|------|-----|
| 文件名 | `release.keystore` |
| 密钥库类型 | PKCS12 |
| 算法 | RSA 2048-bit |
| 签名算法 | SHA256withRSA |
| 有效期 | 2026-05-24 至 2053-10-09 (10,000天) |

### 密钥库密码
```
密钥库密码: idocar123
密钥别名: idocar
密钥密码: idocar123
```

### 证书指纹
| 类型 | 指纹 |
|------|------|
| **SHA1** | `54:CC:70:37:CB:88:88:B8:4C:29:18:1F:89:E5:93:F9:67:D2:80:E3` |
| **SHA256** | `9C:FE:8A:75:27:DE:4C:BE:87:C9:C1:6D:54:FD:A0:BA:24:EB:2E:AE:37:86:24:9C:81:67:CE:08:9A:C7:02:96` |

### 证书所有者信息
```
CN=IdoCar Launcher
OU=IdoCar
O=IdoCar Inc
L=Shenzhen
ST=Guangdong
C=CN
```

---

## GitHub Secrets 配置

在仓库 Settings → Secrets → Actions 中添加以下 Secrets：

| Secret 名称 | 值 |
|-------------|-----|
| `RELEASE_KEYSTORE_BASE64` | 见下方 Base64 编码 |
| `KEYSTORE_PASSWORD` | `idocar123` |
| `KEY_ALIAS` | `idocar` |
| `KEY_PASSWORD` | `idocar123` |

### Base64 编码（完整）
```
MIIKxAIBAzCCCm4GCSqGSIb3DQEHAaCCCl8EggpbMIIKVzCCBa4GCSqGSIb3DQEHAaCCBZ8EggWbMIIFlzCCBZMGCyqGSIb3DQEMCgECoIIFQDCCBTwwZgYJKoZIhvcNAQUNMFkwOAYJKoZIhvcNAQUMMCsEFG8HPtSGTcr9DfFv1MrL91cB16k8AgInEAIBIDAMBggqhkiG9w0CCQUAMB0GCWCGSAFlAwQBKgQQ+warre8aKuLexxz6DdkSrwSCBNDwop75PrGg+0si8M4cjOXGw+6THO47ed9AF9GF3f9bOYAGExzQbjpqAggJhTGcUyBZLen7B3beu4tZLduSRUNr01azkSGRq3TcSO8J9RJOV2a6/ph4QzJiNcw2Z9aFDw+U3dgXEca0bXuf1wQT9Lx2SwoA5dG/RZsvbsyE8NwjHcr4l/cj7d+A+CGng0rXWDwOQMj/kW34X9PkCkERDqogr+CbhthON8vAzdp1WGZ9pRGUt3ul2ij4OwcI0wrAv4ybIJzcH7pKrCDCya51S3dNwZcUx/IuRKK4dBqwHytRqdpgFHwI07/TVOhVbCwMufxjVZIgR44vEGDFeCK3m1P6ECea1O9ziu/OhU7hGS9uzPu/ouo99I07EwcLrmO7ZCBkEzEpcpZ9EMeNnrnKCt4QO452FeaKUVDQE5rlFq9kI06a9BSG1BS15So6SsIH1Wn8cZRcKBlrk2j1RJigVVUb1z/rtf4mRF50FWDlIrvNvl8RwWQOrP333qfNqTlEHSTpCbY/GddPdffVTfq3NJKoWlAHUv8rkrndcpfIxxNA79PisIbIVRx+3YVMXGkJf/fd1DBMGGV98IG0f/cRh6A9j0BOo87sQyc4Y3ZqD9U+JxE0R/HzbjRcmLa4uEyk2Zw9pPIyN425LvJ/x1m/zcQfPSiqUkbzIOYJNoNTEDYmXJLYXdT6yWNC+BorV0bQHvG5BbVRJl+W0iW/f1KgFnKwLcaDYJ5dVaCa/gYtkq+34RMCRhKGKICRkcrQGwbCs3tn3TU76y6eJ5pvc8TG8LDNX79DrzIweD3m8gGWJHBNaAmnvq+KwS5xYs/QVhNcPkztrAFkJow9f7IjZ0mclgWDFVH2g9PDki3tDi/x8C/5syBCWuCVycpmQCY9KnxJ/HQ78rCv50nhHltXMCA6mYAO3Bmp7bBBPkyi+Bpv8iW8kyatZss3cMN6F8NRbmOpcPPXcOV0BpXrxg3lW1bolrjYyVpM5uB7ESm3vpVkMuB3ZaMMd+LXOq1jy3Zm8VQYvoAFDUPz49/Re6w5tNDepZOJUNt9ElBJz7Lt2/ir8vxOqSTSZEK/YzCSwBPDt3r4/eNssxPWLcclsbCnT2nFgJ4b5QLt9+qiL2AqWYpnRCSYXVGeBvu9hQKy26k9h2Qrn/+Dxs3grPYfdgB3CYXxscUq5tRxeZdGInl18PAZZJAUvoBOR1OWhBwWwE+vSyTY93ny/J1wlMfxCS1mp42zOFN3zDJdUV4QS522HaTXuURzyrF4up+8FdjD904ZD9kE9qzybr4WO/xB0w4urq8NzXJpJtEMbGto+O+Oys074zV1eYsGh6EITVVtSgEclOSxRwEg9Fwvg9f0E7C6E0DFjybSa/tXHXWjuQZXJ+AubDedWavnE08uLUKSyX+vPRikgUhGH2KDbz6jh7vHan7px9ELrdkQEFu6B3JuALtoLqVIAiu1NcSsASnSnWihaXGfAus+Yn/xGeZB/ti1VRRazwGk5/O/vo92vAa70fvjS5vPCJxwN/UhL0gqsmnsgtzyyRzkVvBz0t15pEX4BP3EgbS/hvLKkMayBZD6apKVaAzb3eecgWsY5o87bb7FLPl00zW01LmHlJnj0V0oeu+0j/NFyMKYQQcefVKTEwBRfFzf9DFAMBsGCSqGSIb3DQEJFDEOHgwAaQBkAG8AYwBhAHIwIQYJKoZIhvcNAQkVMRQEElRpbWUgMTc3OTYyOTc5ODQ0NjCCBKEGCSqGSIb3DQEHBqCCBJIwggSOAgEAMIIEhwYJKoZIhvcNAQcBMGYGCSqGSIb3DQEFDTBZMDgGCSqGSIb3DQEFDDArBBRfAVaRKGtwf9bM4CyliCixk3bXeAICJxACASAwDAYIKoZIhvcNAgkFADAdBglghkgBZQMEASoEEJHsAUb7xDYqPciG39i+kzuAggQQZwvJ52zj5v1Y3wpHcMKMz4F0w5A1GEHiJQiqH1cdFuE2vQCxioNMd6VoY7HKnJiBcESgTO4McjO27pTILC0aA1nmTRY6otZ3LOI4J18Dxq2XoucLN8o029QpNtwTePTyxr+uNQtXAh8AWKzLypvAkalB01qEj/C9G+mUY189c8TLJ1tQI7E7UK7YNmlQf0Pn0NacSu53s1cVKfQJk/6713vnp1BtTGPU5sMZEOIEe6ML0GdJUNUOH359odEfDsAUlqGj7BRYvTUWMhaFr3mtK5MJxZAsTecOf4qgledZCJj/sMgqgxAXvxrfNxrkP0uTn1QH+EB+dhZ752zHFrL8xF6hb4oU8bxiMnBq/5L5kikk2K6PcGbBBQ384RcOuP5dOszLZ14NTAGPuKc0gkS8gfBOms8TQqqn70pDR1JDCMxmyee8dBNjCBUMm79j56u1iz5EJaYAUx06bR7+zEGXhiE806zWnxT+8SwUFvwnFeeN0aYVVEpgqpTI/CQBZoLLx0ZvpWwkmmhyf9PSNSfdfNDvfksKriKSkQyHOEv+uRwucEMbP2Sd1UX8+Nh3lRpyspJc1o7An6UIfBBpEB5u5ZNMxcBV2krA0fjk1LzDKU50WKhiN9dbD73/+vK32HRMOGMrOQY6i7rTwOmw+0jfUG0u7mdEKYTCPTkwXZhkgGYssAf7IEUBh46ikm1XRJj1B0or8WGo3PHesDZvgmDX/GybrSh5BwKK2pepa15+P8P9FtSyTNVDzMfnFJsQ1sXN37IEQ5lZzJAw5cFBGQppviREZdZvteeg9xYpPK6FFYbGli5aYNmhWxQqdcDsaxNN6VSdMtec5FSoc5apY82OX5Y6UUIUS+JDyjvLZW4U2yQpOYSdGDTm1RHXm395AuNiahjltV9Gz4Xgr9cXxm2MS0383xpLAb/O6fB0JjaifJN19/NepghGOZ1wr2S8GF+tT7Yx5aeO1HqADN95hqH6YU/izCW0ZviUXPoZS5H4IiHkxLs9NOaMHd/rN7RQZUf3T9JyNgkqwjCiYK091HGihqhKLGmBZSnM7q4UJ6Qsb44mXONpvKS+zEnSzZ67nUSCZBOyOP63u5353u64BGE2CfKV7QnEJFyUpbdsB4G7IG4/LfPly3nTppqEr7f4wWyObvEdiQN2YmTIvrAaW04YgnVBvR5oQnmL86Yfa3Jqpy/jhxVT9jBnvwTQBYC/vqA7SHMUbWfn/4yayZKkEiscziq1qJXqNWkAOUyc3WafXqnsNSq6JZZY7gtlbJdVZs8kowPkjbrSEIugkOyueeBojqXdK22ElmgUTUT3ZntA9P+EjHlok04cxbHGUM61gzGv4sTXJUdvtL3OEATo8fuNJbLw5LJIN0LMjOXgJ8s+c8owTTAxMA0GCWCGSAFlAwQCAQUABCB3uU0Qcm3lblP6xGCTAq+ssYzGaxZN2VK0GeLPZtPu3AQUUNKjlPkyFwrb4WLTgFV5q2GhD58CAicQ
```

---

## 生成命令参考

如需重新生成签名文件：

```bash
# 生成密钥库
keytool -genkeypair \
  -v \
  -keystore release.keystore \
  -alias idocar \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass idocar123 \
  -keypass idocar123 \
  -dname "CN=IdoCar Launcher, OU=IdoCar, O=IdoCar Inc, L=Shenzhen, ST=Guangdong, C=CN"

# 查看签名信息
keytool -list -v -keystore release.keystore -storepass idocar123

# 转换为 Base64
base64 -w 0 release.keystore
```

---

## 本地构建签名

使用 Gradle 命令行签名构建：

```bash
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=release.keystore \
  -Pandroid.injected.signing.store.password=idocar123 \
  -Pandroid.injected.signing.key.alias=idocar \
  -Pandroid.injected.signing.key.password=idocar123
```

---

*文档生成时间: 2026-05-24*
