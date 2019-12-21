package qa.justtestlah.mobile.tools;

/** DTO to hold application meta information. */
public class ApplicationInfo {

	  private String applicationName;
	  private String versionName;
	  private String versionCode;
	
  /** Return the full application information. */
  @Override
  public String toString() {
    return applicationName + " " + versionName + "_" + versionCode;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public String getVersionName() {
    return versionName;
  }

  public void setVersionName(String versionName) {
    this.versionName = versionName;
  }

  public String getVersionCode() {
    return versionCode;
  }

  public void setVersionCode(String versionCode) {
    this.versionCode = versionCode;
  }

  public ApplicationInfo(String applicationName, String versionName, String versionCode) {
    super();
    this.applicationName = applicationName;
    this.versionName = versionName;
    this.versionCode = versionCode;
  }
}
