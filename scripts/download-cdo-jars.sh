#!/bin/bash
# 下载CDO JAR文件 - 从Eclipse官方仓库

CDO_DIR="/mnt/d/sysml2/server/libs/cdo"
mkdir -p $CDO_DIR
cd $CDO_DIR

# Eclipse 2023-06版本的CDO
ECLIPSE_REPO="https://download.eclipse.org/releases/2023-06"

echo "Downloading CDO from Eclipse 2023-06 release..."

# 下载compositeContent.jar获取插件列表
wget -q -O compositeContent.jar "${ECLIPSE_REPO}/compositeContent.jar"
if [ -f compositeContent.jar ]; then
    unzip -l compositeContent.jar
fi

# 直接尝试CDO特定版本的URL
CDO_VERSION="4.23.0"
CDO_DATE="v20230605"

# CDO核心JAR文件列表
declare -A CDO_JARS=(
    ["org.eclipse.emf.cdo"]="${CDO_VERSION}.${CDO_DATE}-1521"
    ["org.eclipse.emf.cdo.common"]="${CDO_VERSION}.${CDO_DATE}-1238"
    ["org.eclipse.emf.cdo.server"]="4.12.0.v20230601-1511"
    ["org.eclipse.net4j"]="4.19.0.${CDO_DATE}-1238"
    ["org.eclipse.net4j.util"]="3.23.0.${CDO_DATE}-1238"
)

# 从不同的可能位置尝试下载
for jar_name in "${!CDO_JARS[@]}"; do
    version="${CDO_JARS[$jar_name]}"
    filename="${jar_name}_${version}.jar"
    
    echo "Trying to download $filename..."
    
    # 尝试多个URL模式
    urls=(
        "https://download.eclipse.org/modeling/emf/cdo/updates/releases/4.23/plugins/${filename}"
        "https://download.eclipse.org/modeling/emf/cdo/drops/R20230612-0935/plugins/${filename}"
        "https://ftp.fau.de/eclipse/modeling/emf/cdo/drops/R20230612-0935/plugins/${filename}"
        "https://mirror.umd.edu/eclipse/modeling/emf/cdo/drops/R20230612-0935/plugins/${filename}"
    )
    
    for url in "${urls[@]}"; do
        echo "  Trying: $url"
        if wget -q -O "${jar_name}.jar" "$url"; then
            # 检查是否是真的JAR文件
            if file "${jar_name}.jar" | grep -q "Java archive"; then
                echo "  ✓ Downloaded ${jar_name}.jar"
                break
            elif file "${jar_name}.jar" | grep -q "Zip archive"; then
                echo "  ✓ Downloaded ${jar_name}.jar (ZIP format)"
                break
            else
                echo "  ✗ Not a JAR file, trying next URL..."
                rm -f "${jar_name}.jar"
            fi
        fi
    done
done

echo "Checking downloaded files..."
ls -lh *.jar 2>/dev/null || echo "No JAR files downloaded"
