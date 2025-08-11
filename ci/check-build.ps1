param()
Write-Host "[check-build] Start"
# 占位：探测 Gradle/Maven 可用性
try { & gradle -v | Out-Null } catch { Write-Host "Gradle not found, skip" }
try { & mvn -v | Out-Null } catch { Write-Host "Maven not found, skip" }
Write-Host "[check-build] OK"
exit 0



