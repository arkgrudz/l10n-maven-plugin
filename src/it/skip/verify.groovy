log = new File(basedir, "build.log")
assert log.getText().contains("Skipping plugin execution, as per configuration");