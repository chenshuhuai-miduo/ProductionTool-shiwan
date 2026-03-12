package com.miduo.cloud.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.util.Arrays;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * MyBatis 配置类
 * 用于解决 JTDS 驱动不支持 LocalDateTime 的问题
 */
@Configuration
public class MyBatisConfig {

    @Autowired
    private Environment environment;

    /**
     * 配置 SqlSessionFactory，注册自定义类型处理器
     */
    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        
        // 加载 Mapper XML 文件
        sessionFactory.setMapperLocations(
            new PathMatchingResourcePatternResolver().getResources("classpath*:/mapper/**/*.xml")
        );
        
        // 设置类型别名包（同时支持旧实体和新实体）
        sessionFactory.setTypeAliasesPackage("com.company.productionline.entity,com.miduo.cloud.entity.po");
        
        // MyBatis-Plus 全局配置
        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(com.baomidou.mybatisplus.annotation.IdType.AUTO);
        dbConfig.setLogicDeleteField("isDel");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        sessionFactory.setGlobalConfig(globalConfig);
        
        // MyBatis 配置
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(false);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        configuration.setLogImpl(StdOutImpl.class);
        
        // 注册 LocalDateTime 类型处理器（关键配置）
        configuration.getTypeHandlerRegistry().register(LocalDateTime.class, new JtdsLocalDateTimeTypeHandler());
        configuration.setDefaultScriptingLanguage(org.apache.ibatis.scripting.xmltags.XMLLanguageDriver.class);
        
        sessionFactory.setConfiguration(configuration);
        
        // MyBatis-Plus 插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 石湾 2 号机使用 MySQL；其他（致美斋等）使用 SQL Server 2005
        DbType dbType = Arrays.stream(environment.getActiveProfiles()).anyMatch("shiwan-m2"::equals)
            ? DbType.MYSQL
            : DbType.SQL_SERVER2005;
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(dbType);
        // 设置最大单页限制数量，默认 500 条，为了支持 Bloom Filter 初始化大批量加载，提升到 200000 条
        paginationInterceptor.setMaxLimit(200000L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        sessionFactory.setPlugins(interceptor);
        
        return sessionFactory.getObject();
    }

    /**
     * LocalDateTime 类型处理器（兼容 JTDS 驱动）
     * 将 LocalDateTime 转换为 Timestamp 进行存储
     */
    public static class JtdsLocalDateTimeTypeHandler implements TypeHandler<LocalDateTime> {

        @Override
        public void setParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType) throws SQLException {
            if (parameter == null) {
                ps.setTimestamp(i, null);
            } else {
                ps.setTimestamp(i, Timestamp.valueOf(parameter));
            }
        }

        @Override
        public LocalDateTime getResult(ResultSet rs, String columnName) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(columnName);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }

        @Override
        public LocalDateTime getResult(ResultSet rs, int columnIndex) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(columnIndex);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }

        @Override
        public LocalDateTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
            Timestamp timestamp = cs.getTimestamp(columnIndex);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }
    }
}

