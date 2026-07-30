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

}
