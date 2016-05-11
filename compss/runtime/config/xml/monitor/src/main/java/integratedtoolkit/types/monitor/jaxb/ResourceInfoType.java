//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.05.06 at 03:57:09 PM BRT 
//


package integratedtoolkit.types.monitor.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ResourceInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ResourceInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Resource" type="{}ResourceType" maxOccurs="unbounded"/>
 *         &lt;element name="AccumulatedCost" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResourceInfoType", propOrder = {
    "resource",
    "accumulatedCost"
})
public class ResourceInfoType {

    @XmlElement(name = "Resource", required = true)
    protected List<ResourceType> resource;
    @XmlElement(name = "AccumulatedCost")
    protected float accumulatedCost;

    /**
     * Gets the value of the resource property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the resource property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getResource().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ResourceType }
     * 
     * 
     */
    public List<ResourceType> getResource() {
        if (resource == null) {
            resource = new ArrayList<ResourceType>();
        }
        return this.resource;
    }

    /**
     * Gets the value of the accumulatedCost property.
     * 
     */
    public float getAccumulatedCost() {
        return accumulatedCost;
    }

    /**
     * Sets the value of the accumulatedCost property.
     * 
     */
    public void setAccumulatedCost(float value) {
        this.accumulatedCost = value;
    }

}
