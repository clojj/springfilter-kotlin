package com.example.springfilterkotlin

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.javafaker.Faker
import com.turkraft.springfilter.builder.FilterBuilder
import com.turkraft.springfilter.converter.FilterSpecification
import com.turkraft.springfilter.converter.FilterSpecificationConverter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException


@SpringBootApplication
@RestController
class SpringfilterKotlinApplication(
    private val industryRepository: IndustryRepository,
    private val companyRepository: CompanyRepository,
    private val filterBuilder: FilterBuilder,
    private val filterSpecificationConverter: FilterSpecificationConverter,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val faker = Faker(java.util.Random(1))
        val industries: MutableList<Industry> = ArrayList()
        for (i in 0..2) {
            val industry = Industry()
            industry.name = faker.company().industry()
            industries.add(industry)
        }
        industryRepository.saveAll(industries)
        val companies: MutableList<Company> = ArrayList()
        for (i in 0..4) {
            val company = Company()
            company.name = faker.company().name()
            company.industry = faker.options().nextElement(industries)
            companies.add(company)
        }
        companyRepository.saveAll(companies)
    }

    @GetMapping("/")
    @Throws(IOException::class)
    fun index(response: HttpServletResponse) {
        response.sendRedirect("swagger-ui.html")
    }

    @Operation(parameters = [Parameter(name = "filter", `in` = ParameterIn.QUERY, schema = Schema(type = "string"), example = "industry.name:'Capital Markets'")])
    @GetMapping(value = ["company"])
    fun getCompanies(@Parameter(hidden = true) filter: FilterSpecification<Company?>): List<Company?> {
        return companyRepository.findAll(filter)
    }

    @Operation(parameters = [Parameter(name = "filter", `in` = ParameterIn.QUERY, schema = Schema(type = "string"), example = "industry:Capital%20Markets'")])
    @GetMapping(value = ["companyDto"])
    fun getCompaniesDto(@Parameter(hidden = true) filter: String): List<Company?> {
        // TODO transform parameter
        val filterNode = with (filterBuilder) {
            field("industry.name").equal(input("Capital Markets")).get()
        }
        val specification: FilterSpecification<Company> = filterSpecificationConverter.convert(filterNode)
        return companyRepository.findAll(specification)
    }

    @GetMapping(value = ["industry"])
    fun getIndustries(
        @Parameter(hidden = true) filter: FilterSpecification<Industry?>?
    ): MutableList<Industry?>? {
        return filter?.let { industryRepository.findAll(it) }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SpringfilterKotlinApplication::class.java, *args)
        }
    }
}

@Repository
interface IndustryRepository : CrudRepository<Industry?, Int?>, JpaSpecificationExecutor<Industry?>

@Repository
interface CompanyRepository : CrudRepository<Company?, Int?>, JpaSpecificationExecutor<Company?>

@Entity
@Table
class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null
    var name: String? = null

    @JsonIgnoreProperties("companies")
    @ManyToOne
    var industry: Industry? = null

}

@Entity
@Table
class Industry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null
    var name: String? = null

}
