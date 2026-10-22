// Called with JOBS = [{ src: "...skeleton-400.svg", out: "...outlined-400.svg" }, ...] defined before this file.
function outlineJob(job) {
  var doc = app.open(File(job.src));
  var groups = [];
  for (var i = 0; i < doc.groupItems.length; i++) {
    var g = doc.groupItems[i];
    if (g.parent.typename === "Layer") groups.push(g);
  }
  for (var k = 0; k < groups.length; k++) {
    var grp = groups[k];
    var name = grp.name;
    doc.selection = null;
    var stroked = 0;
    for (var p = 0; p < grp.pathItems.length; p++) {
      if (grp.pathItems[p].stroked) {
        grp.pathItems[p].selected = true;
        stroked++;
      }
    }
    if (stroked > 0) app.executeMenuCommand("OffsetPath v22");
    doc.selection = null;
    grp.selected = true;
    app.executeMenuCommand("Live Pathfinder Add");
    app.executeMenuCommand("expandStyle");
    doc.selection[0].name = name;
  }
  doc.selection = null;
  var so = new ExportOptionsSVG();
  so.coordinatePrecision = 2;
  so.cssProperties = SVGCSSPropertyLocation.PRESENTATIONATTRIBUTES;
  so.documentEncoding = SVGDocumentEncoding.UTF8;
  so.fontType = SVGFontType.OUTLINEFONT;
  doc.exportFile(File(job.out), ExportType.SVG, so);
  doc.close(SaveOptions.DONOTSAVECHANGES);
  return groups.length;
}

(function () {
  app.userInteractionLevel = UserInteractionLevel.DONTDISPLAYALERTS;
  var log = [];
  for (var j = 0; j < JOBS.length; j++) {
    try {
      log.push(JOBS[j].out + ": " + outlineJob(JOBS[j]));
    } catch (e) {
      log.push("ERR " + JOBS[j].src + " line " + e.line + ": " + e.message);
    }
  }
  return log.join("\n");
})();
