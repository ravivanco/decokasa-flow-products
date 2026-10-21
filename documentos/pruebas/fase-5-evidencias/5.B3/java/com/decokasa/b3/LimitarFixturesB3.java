package com.decokasa.b3;
import java.nio.file.*;
import javax.sql.DataSource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Aislamiento opt-in: renueva el contexto antes de que fixtures pequeños se acumulen; no altera aserciones. */
public class LimitarFixturesB3 extends AbstractTestExecutionListener {
 @Override public int getOrder(){return 1000;}
 @Override public void afterTestMethod(TestContext t)throws Exception{
  if(!t.hasApplicationContext())return;
  var ctx=t.getApplicationContext();
  var bases=ctx.getBeansOfType(PostgreSQLContainer.class);
  if(bases.isEmpty()||ctx.getBeansOfType(DataSource.class).isEmpty())return;
  var pg=bases.values().iterator().next();
  int pedidos;
  try(var c=ctx.getBean(DataSource.class).getConnection()){
   if(!c.getMetaData().getURL().contains(":"+pg.getMappedPort(5432)+"/"))throw new IllegalStateException("Base ajena al contenedor de prueba");
   try(var s=c.createStatement();var rs=s.executeQuery("select to_regclass('public.pedido')")){rs.next();if(rs.getString(1)==null)return;}
   try(var s=c.createStatement();var rs=s.executeQuery("select count(*) from pedido")){rs.next();pedidos=rs.getInt(1);}
  }
  String destino=System.getProperty("b3.conteos");
  if(destino!=null)Files.writeString(Path.of(destino),"{\"clase\":\""+t.getTestClass().getSimpleName()+"\",\"metodo\":\""+t.getTestMethod().getName()+"\",\"pedidos\":"+pedidos+"}\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
  if(pedidos>20)throw new AssertionError("Fixture supera máximo autorizado de 20 pedidos");
  if(pedidos>=8){
   t.markApplicationContextDirty(HierarchyMode.CURRENT_LEVEL);
   t.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,Boolean.TRUE);
  }
 }
}
