//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.01.18 at 11:10:40 AM GMT
//

package net.sf.mpxj.mspdi.schema;

import javax.xml.bind.annotation.adapters.XmlAdapter;

@SuppressWarnings("all") public class Adapter12 extends XmlAdapter<String, Integer>
{

   public Integer unmarshal(String value)
   {
      return (net.sf.mpxj.mspdi.DatatypeConverter.parseResourceUID(value));
   }

   public String marshal(Integer value)
   {
      return (net.sf.mpxj.mspdi.DatatypeConverter.printResourceUID(value));
   }

}
