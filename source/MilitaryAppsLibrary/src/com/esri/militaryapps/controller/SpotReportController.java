/**
 * *****************************************************************************
 * Copyright 2013-2015 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *****************************************************************************
 */
package com.esri.militaryapps.controller;

import com.esri.militaryapps.model.DomNodeAndDocument;
import com.esri.militaryapps.model.Geomessage;
import com.esri.militaryapps.model.SpotReport;
import com.esri.militaryapps.util.Utilities;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A controller that broadcasts spot reports.
 */
public class SpotReportController {
    
    /**
     * The type string for this controller's Geomessages.
     */
    public static final String REPORT_TYPE = "spot_report";

    private static final Logger logger = Logger.getLogger(SpotReportController.class.getName());
    private final MapController mapController;
    private final MessageController messageController;

    /**
     * Creates a new SpotReportController.
     * @param mapController the application's map controller, for converting coordinates.
     * @param messageController the controller that sends messages to listening clients.
     */
    public SpotReportController(
            MapController mapController,
            MessageController messageController) {
        this.mapController = mapController;
        this.messageController = messageController;
    }

    /**
     * Sends a new spot report out to UDP clients. This method simply calls
     * submitSpotReport(spotReport, false).
     * @param spotReport the spot report to send.
     * @throws IOException
     */
    public void sendSpotReport(SpotReport spotReport, String uniqueDesignation)
            throws IOException, ParserConfigurationException, TransformerException {
        sendSpotReport(spotReport, uniqueDesignation, false);
    }

    /**
     * Sends a spot report out to UDP clients.
     * @param spotReport the spot report to send.
     * @param isUpdate false if the spot report's ID should be regenerated so as
     * to be a new unique spot report; true otherwise.
     * @throws IOException
     */
    public void sendSpotReport(SpotReport spotReport, String uniqueDesignation, boolean isUpdate)
            throws IOException, ParserConfigurationException, TransformerException {
        if (null != spotReport) {
            if (!isUpdate) {
                spotReport.regenerateMessageId();
            }
            messageController.sendMessage(getSpotReportAsString(spotReport, uniqueDesignation).getBytes());
        }
    }

    /**
     * Converts a SpotReport to a string.
     * @param spotReport the SpotReport.
     * @param senderUniqueDesignation a unique designation for the sender. If null,
     *                                the uniquedesignation field will be omitted
     *                                from the returned string.
     * @return the string.
     * @throws ParserConfigurationException 
     * @throws TransformerException
     */
    public String getSpotReportAsString(SpotReport spotReport, String senderUniqueDesignation)
            throws ParserConfigurationException, TransformerException {
        Date theTime;
        if (null != spotReport.getTime()) {
            theTime = spotReport.getTime().getTime();
        } else {
            theTime = new Date();
        }

        DomNodeAndDocument nodeAndDocument = Utilities.createGeomessageDocument();
        Document doc = nodeAndDocument.getDocument();
        Node geomessageElement = nodeAndDocument.getNode();
        
        Utilities.addTextElement(doc, geomessageElement,
                Geomessage.TYPE_FIELD_NAME, AdvancedSymbolController.getOutboundMessageTypeName(REPORT_TYPE));
        Utilities.addTextElement(doc, geomessageElement,
                Geomessage.ID_FIELD_NAME, spotReport.getMessageId());
        Utilities.addTextElement(doc, geomessageElement,
                Geomessage.WKID_FIELD_NAME,
                Integer.toString(spotReport.getLocationWkid()));
        Utilities.addTextElement(doc, geomessageElement,
                Geomessage.CONTROL_POINTS_FIELD_NAME,
                spotReport.getLocationX() + "," + spotReport.getLocationY());
        Utilities.addTextElement(doc, geomessageElement,
                Geomessage.ACTION_FIELD_NAME, "update");
        if (null != senderUniqueDesignation) {
            Utilities.addTextElement(doc, geomessageElement, "uniquedesignation",
                    senderUniqueDesignation);
        }

        // salute format attributes
        Utilities.addTextElement(doc, geomessageElement, "size", spotReport.getSize().toString());
        Utilities.addTextElement(doc, geomessageElement, "activity", spotReport.getActivity().toString());
        Utilities.addTextElement(doc, geomessageElement, "location", mapController.pointToMgrs(
                spotReport.getLocationX(),
                spotReport.getLocationY(),
                spotReport.getLocationWkid()));
        Utilities.addTextElement(doc, geomessageElement, "unit", spotReport.getUnit().toString());
        Utilities.addTextElement(doc, geomessageElement, "equipment", spotReport.getEquipment().toString());

        Utilities.addTextElement(doc, geomessageElement, "size_cat", Integer.toString(spotReport.getSize().getCode()));
        Utilities.addTextElement(doc, geomessageElement, "activity_cat", spotReport.getActivity().getCode());
        Utilities.addTextElement(doc, geomessageElement, "unit_cat", spotReport.getUnit().getCode());
        Utilities.addTextElement(doc, geomessageElement, "equip_cat", spotReport.getEquipment().getCode());
        Utilities.addTextElement(doc, geomessageElement, "timeobserved", Utilities.DATE_FORMAT_GEOMESSAGE.format(theTime));
        Utilities.addTextElement(doc, geomessageElement, "datetimesubmitted", Utilities.DATE_FORMAT_GEOMESSAGE.format(new Date()));
        
        return Utilities.documentToString(doc);
    }
    
}
