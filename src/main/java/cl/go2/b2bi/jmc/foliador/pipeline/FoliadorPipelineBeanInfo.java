package cl.go2.b2bi.jmc.foliador.pipeline;
import java.beans.BeanDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.beans.IntrospectionException;

public class FoliadorPipelineBeanInfo extends SimpleBeanInfo{

    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor descriptor = new BeanDescriptor(FoliadorPipeline.class);
        descriptor.setName("Foliador_pipeline");
        descriptor.setShortDescription("Generador de folios para SINACOFI/CMF");
        return descriptor;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor pathDescriptor = new PropertyDescriptor("configFilePath", FoliadorPipeline.class);
            pathDescriptor.setShortDescription("Ruta completa al archivo de propiedades conexión a DB)");



            return new PropertyDescriptor[] { pathDescriptor };
        } catch (IntrospectionException e) {
            throw new RuntimeException("Error inicializando propiedades del JMC", e);
        }
    }

}
