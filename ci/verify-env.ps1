param()
Write-Host "[verify-env] Start"
Write-Host "[verify-env] JAVA_HOME=$env:JAVA_HOME"
try { java -version } catch { Write-Host "java not found" }
try { gradle -v } catch { Write-Host "gradle not found" }
try { mvn -v } catch { Write-Host "maven not found" }
try { node -v; npm -v } catch { Write-Host "node/npm not found" }
Write-Host "[verify-env] Done"
exit 0


