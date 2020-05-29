/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc.projection.sat;

import ucar.unidata.geoloc.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for finding the ProjectionRect from a LatLonRect
 *
 * @author caron
 * @since 7/29/2014
 */
public class BoundingBoxHelper {

  private double maxR, maxR2;
  private Projection proj;

  public BoundingBoxHelper(Projection proj, double maxR) {
    this.proj = proj;
    this.maxR = maxR;
    this.maxR2 = maxR * maxR;
  }

  public ProjectionRect latLonToProjBB(LatLonRect rect) {

    ProjectionPoint llpt = proj.latLonToProj(rect.getLowerLeftPoint());
    ProjectionPoint urpt = proj.latLonToProj(rect.getUpperRightPoint());
    ProjectionPoint lrpt = proj.latLonToProj(rect.getLowerRightPoint());
    ProjectionPoint ulpt = proj.latLonToProj(rect.getUpperLeftPoint());

    // how many are bad?
    List<ProjectionPoint> goodPts = new ArrayList<>(4);
    int countBad = 0;
    if (!addGoodPts(goodPts, llpt))
      countBad++;
    if (!addGoodPts(goodPts, urpt))
      countBad++;
    if (!addGoodPts(goodPts, lrpt))
      countBad++;
    if (!addGoodPts(goodPts, ulpt))
      countBad++;

    // case : 3 or 4 good points, just use those

    // case: only 2 good ones : extend to edge of the limit circle
    if (countBad == 2) {

      if (!LatLonPoints.isInfinite(llpt) && !LatLonPoints.isInfinite(lrpt)) {
        addGoodPts(goodPts, ProjectionPoint.create(0, maxR));

      } else if (!LatLonPoints.isInfinite(ulpt) && !LatLonPoints.isInfinite(llpt)) {
        addGoodPts(goodPts, ProjectionPoint.create(maxR, 0));

      } else if (!LatLonPoints.isInfinite(ulpt) && !LatLonPoints.isInfinite(urpt)) {
        addGoodPts(goodPts, ProjectionPoint.create(0, -maxR));

      } else if (!LatLonPoints.isInfinite(urpt) && !LatLonPoints.isInfinite(lrpt)) {
        addGoodPts(goodPts, ProjectionPoint.create(-maxR, 0));

      } else {
        throw new IllegalStateException();
      }

    } else if (countBad == 3) { // case: only 1 good one : extend to wedge of the limit circle

      if (!LatLonPoints.isInfinite(llpt)) {
        double xcoord = llpt.getX();
        addGoodPts(goodPts, ProjectionPoint.create(xcoord, getLimitCoord(xcoord)));

        double ycoord = llpt.getY();
        addGoodPts(goodPts, ProjectionPoint.create(getLimitCoord(ycoord), ycoord));
      } else if (!LatLonPoints.isInfinite(urpt)) {
        double xcoord = urpt.getX();
        addGoodPts(goodPts, ProjectionPoint.create(xcoord, -getLimitCoord(xcoord)));

        double ycoord = urpt.getY();
        addGoodPts(goodPts, ProjectionPoint.create(-getLimitCoord(ycoord), ycoord));
      } else if (!LatLonPoints.isInfinite(ulpt)) {
        double xcoord = ulpt.getX();
        addGoodPts(goodPts, ProjectionPoint.create(xcoord, -getLimitCoord(xcoord)));

        double ycoord = ulpt.getY();
        addGoodPts(goodPts, ProjectionPoint.create(getLimitCoord(ycoord), ycoord));
      } else if (!LatLonPoints.isInfinite(lrpt)) {
        double xcoord = lrpt.getX();
        addGoodPts(goodPts, ProjectionPoint.create(xcoord, getLimitCoord(xcoord)));

        double ycoord = lrpt.getY();
        addGoodPts(goodPts, ProjectionPoint.create(-getLimitCoord(ycoord), ycoord));

      } else {
        throw new IllegalStateException();
      }

    }

    return makeRect(goodPts);
  }

  private boolean addGoodPts(List<ProjectionPoint> goodPts, ProjectionPoint pt) {
    if (!LatLonPoints.isInfinite(pt)) {
      goodPts.add(pt);
      return true;
    } else
      return false;
  }

  // where does line x|y = coord intersect the map limit circle?
  // return the positive root.
  private double getLimitCoord(double coord) {
    return Math.sqrt(maxR2 - coord * coord);
  }

  private ProjectionRect makeRect(List<ProjectionPoint> goodPts) {
    double minx = Double.MAX_VALUE;
    double miny = Double.MAX_VALUE;
    double maxx = -Double.MAX_VALUE;
    double maxy = -Double.MAX_VALUE;
    for (ProjectionPoint pp : goodPts) {
      minx = Math.min(minx, pp.getX());
      maxx = Math.max(maxx, pp.getX());
      miny = Math.min(miny, pp.getY());
      maxy = Math.max(maxy, pp.getY());
    }
    return new ProjectionRect(minx, miny, maxx, maxy);
  }
}
