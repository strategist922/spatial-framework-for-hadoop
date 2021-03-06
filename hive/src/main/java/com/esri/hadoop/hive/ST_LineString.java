package com.esri.hadoop.hive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.serde2.io.DoubleWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hive.pdk.HivePdkUnitTest;
import org.apache.hive.pdk.HivePdkUnitTests;

import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCPoint;

import java.util.List;


@Description(
	name = "ST_LineString",
	value = "_FUNC_(x, y, [x, y]*) - constructor for 2D line string\n" +
    "_FUNC_('linestring( ... )') - constructor for 2D line string",
	extended = "Example:\n" +
	"  SELECT _FUNC_(1, 1, 2, 2, 3, 3) from src LIMIT 1;\n" + 
	"  SELECT _FUNC_('linestring(1 1, 2 2, 3 3)') from src LIMIT 1;")
@HivePdkUnitTests(
	cases = {
		@HivePdkUnitTest(
			query = "select ST_GeometryType(ST_Linestring('linestring (10 10, 20 20)')) from onerow",
			result = "ST_LINESTRING"
			),
		@HivePdkUnitTest(
			query = "select ST_Equals(ST_Linestring('linestring (10 10, 20 20)'), ST_GeomFromText('linestring (10 10, 20 20)')) from onerow",
			result = "true"
			)
		}
	)

public class ST_LineString extends ST_Geometry {
	static final Log LOG = LogFactory.getLog(ST_LineString.class.getName());

	// Number-pairs constructor
	public BytesWritable evaluate(DoubleWritable ... xyPairs) throws UDFArgumentException{
		
		if (xyPairs == null || xyPairs.length == 0 ||  xyPairs.length%2 != 0) {
			return null;
		}

		try {		
			Polyline linestring = new Polyline();
			linestring.startPath(xyPairs[0].get(), xyPairs[1].get());
		
			for (int i=2; i<xyPairs.length; i+=2) {
				linestring.lineTo(xyPairs[i].get(), xyPairs[i+1].get());
			}
		
			return GeometryUtils.geometryToEsriShapeBytesWritable(OGCGeometry.createFromEsriGeometry(linestring, null));
		} catch (Exception e) {
		    LogUtils.Log_InternalError(LOG, "ST_LineString: " + e);
		    return null;
		}
	}

	// WKT constructor - can use SetSRID on constructed multi-point
	public BytesWritable evaluate(Text wkwrap) throws UDFArgumentException {
		String wkt = wkwrap.toString();
		try {
			OGCGeometry ogcObj = OGCGeometry.fromText(wkt);
			ogcObj.setSpatialReference(null);
			if (ogcObj.geometryType().equals("LineString")) {
				return GeometryUtils.geometryToEsriShapeBytesWritable(ogcObj);
			} else {
				LogUtils.Log_InvalidType(LOG, GeometryUtils.OGCType.ST_LINESTRING, GeometryUtils.OGCType.UNKNOWN);
				return null;
			}

		} catch (Exception e) {  // IllegalArgumentException, GeometryException
			LogUtils.Log_InvalidText(LOG, wkt);
			return null;
		}
	}

	// Arrays constructor.
	// To be complete this should be extended to support other types.
	public BytesWritable evaluate(List<DoubleWritable> x, List<DoubleWritable> y) throws UDFArgumentException {

		if (x == null || y == null || x.size() != y.size()) {
			return null;
		}

		try {		
			Polyline linestring = new Polyline();
			linestring.startPath(x.get(0).get(), y.get(0).get());

			for (int i=1; i<x.size(); i++) {
				linestring.lineTo(x.get(i).get(), y.get(i).get());
			}

			return GeometryUtils.geometryToEsriShapeBytesWritable(OGCGeometry.createFromEsriGeometry(linestring, null));
		} catch (Exception e) {
		    LogUtils.Log_InternalError(LOG, "ST_LineString: " + e);
		    return null;
		}
	}

	// Build a line from an array of points.
	public BytesWritable evaluate(List<BytesWritable> points) throws UDFArgumentException {

		if (points == null || points.size() == 0) {
			return null;
		}

		try {
			Polyline linestring = new Polyline();
			BytesWritable bPoint = points.get(0);
			if (GeometryUtils.getType(bPoint) != GeometryUtils.OGCType.ST_POINT) {
				LogUtils.Log_InvalidType(LOG, GeometryUtils.OGCType.ST_POINT, GeometryUtils.getType(bPoint));
				return null;
			}
			OGCPoint point = (OGCPoint)GeometryUtils.geometryFromEsriShape(bPoint);
			linestring.startPath(point.X(), point.Y());

			for (int i=1; i<points.size(); i++) {
				bPoint = points.get(i);
				if (GeometryUtils.getType(bPoint) != GeometryUtils.OGCType.ST_POINT) {
					LogUtils.Log_InvalidType(LOG, GeometryUtils.OGCType.ST_POINT, GeometryUtils.getType(bPoint));
					return null;
				}
				point = (OGCPoint)GeometryUtils.geometryFromEsriShape(bPoint);
				linestring.lineTo(point.X(), point.Y());
			}
			return GeometryUtils.geometryToEsriShapeBytesWritable(OGCGeometry.createFromEsriGeometry(linestring, null));
		} catch (Exception e) {
		    LogUtils.Log_InternalError(LOG, "ST_LineString: " + e);
		    return null;
		}
	}

}
